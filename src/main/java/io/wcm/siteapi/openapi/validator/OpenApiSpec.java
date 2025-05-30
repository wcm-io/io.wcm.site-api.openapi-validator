/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2023 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.siteapi.openapi.validator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.model.v3.OAI3Context;
import org.openapi4j.core.util.TreeUtil;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults.ValidationItem;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

/**
 * Reads and validates an OAS3 YAML specification.
 * Gives access to {@link OpenApiSchemaValidator} instances for each path/suffix defined in the specification.
 */
public final class OpenApiSpec {

  private final URL url;
  private final String version;
  private final JsonNode rootNode;
  private final ValidationContext<OAI3> validationContext;
  private final ConcurrentMap<String, OpenApiSchemaValidator> validators = new ConcurrentHashMap<>();

  /**
   * Create instance with given spec files.
   * @param path Resource Path to OAS3 spec
   * @param version Spec version or empty string
   * @throws SpecInvalidException If reading OAS3 spec fails.
   */
  public OpenApiSpec(@NotNull String path, @NotNull String version) {
    this(toUrl(path), version);
  }

  private static URL toUrl(@NotNull String path) {
    URL url = OpenApiSpec.class.getClassLoader().getResource(path);
    if (url == null) {
      throw new IllegalArgumentException("File not found in class path: " + path);
    }
    return url;
  }

  /**
   * Create instance with given spec files.
   * @param url URL pointing to OAS3 spec
   * @param version Spec version or empty string
   * @throws SpecInvalidException If reading OAS3 spec fails.
   */
  public OpenApiSpec(@NotNull URL url, @NotNull String version) {
    this.url = url;
    this.version = version;
    try {
      String specContent = readFileContent(url);
      rootNode = TreeUtil.yaml.readTree(specContent);
      OAI3Context apiContext = new OAI3Context(url, rootNode);
      validationContext = new ValidationContext<>(apiContext);
      validateSpec(apiContext, rootNode, url);
    }
    catch (IOException | ResolutionException ex) {
      throw new SpecInvalidException("Unable to load specification " + url + ": " + ex.getMessage(), ex);
    }
  }

  /**
   * Gets YAML content of spec file.
   * @param url Spec URL
   * @return YAML content
   * @throws IOException I/O exception
   */
  private static String readFileContent(@NotNull URL url) throws IOException {
    try (InputStream is = url.openStream()) {
      if (is == null) {
        throw new IllegalArgumentException("File does not exist: " + url);
      }
      String json = IOUtils.toString(is, StandardCharsets.UTF_8);
      /*
       * Apply hotfix to schema JSON - insert slash before ${contentPath} placeholder.
       * The original schema is not a valid because the path keys do not start with "/" -
       * although they actually do when the path parameters are injected, but OAS3 does not
       * support slashes in path parameters yet.
       * See https://github.com/OAI/OpenAPI-Specification/issues/892
       */
      return json.replace("\"{contentPath}", "\"/{contentPath}");
    }
  }

  /**
   * Validates the spec for OAS3 conformance.
   * @param context OAS3 context
   * @param rootNode Spec root node
   * @param url Spec URL
   */
  @SuppressWarnings("null")
  private static void validateSpec(OAI3Context context, JsonNode rootNode, URL url) {
    OpenApi3 api = TreeUtil.json.convertValue(rootNode, OpenApi3.class);
    api.setContext(context);
    try {
      OpenApi3Validator.instance().validate(api);
    }
    catch (ValidationException ex) {
      // put all validation errors in a single message
      StringBuilder result = new StringBuilder();
      result.append(ex.getMessage());
      for (ValidationItem item : ex.results().items()) {
        result.append("\n").append(item.toString());
      }
      throw new SpecInvalidException("Specification is invalid: " + url + " - " + result.toString(), ex);
    }
  }

  /**
   * @return Specification URL.
   */
  public @NotNull URL getURL() {
    return this.url;
  }

  /**
   * @return Spec version (derived from file name) or empty string.
   */
  public @NotNull String getVersion() {
    return this.version;
  }

  /**
   * Get Schema for default response of operation mapped to given suffix.
   *
   * <p>
   * It looks for a path definition ending with <code>/{suffix}.json</code> in the spec
   * and returns the JSON schema defined in the YAML for HTTP 200 GET response with <code>application/json</code>
   * content type.
   * </p>
   *
   * <p>
   * See <a href=
   * "https://github.com/wcm-io/io.wcm.site-api.openapi-validator/blob/develop/src/test/resources/site-api-spec/site-api.yaml">site-api.yaml</a>
   * as minimal example for a valid specification.
   * </p>
   *
   * @param suffix Suffix ID
   * @return Schema JSON node
   */
  public @NotNull OpenApiSchemaValidator getSchemaValidator(@NotNull String suffix) {
    // cache validators per suffixId in map
    return validators.computeIfAbsent(suffix, this::buildSchemaValidator);
  }

  /**
   * Get Schema for default response of operation mapped to given suffix.
   * @param suffix Suffix ID
   * @return Schema JSON node
   */
  private @NotNull OpenApiSchemaValidator buildSchemaValidator(@NotNull String suffix) {
    JsonNode matchingPath = findMatchingPathNode(suffix);
    if (matchingPath == null) {
      throw new IllegalArgumentException("No matching path definition found for suffix: " + suffix);
    }
    // ~1 = / in JSON pointer syntax
    String pointer = "/get/responses/200/content/application~1json/schema";
    JsonNode schemaNode = matchingPath.at(pointer);
    if (schemaNode == null || schemaNode instanceof MissingNode) {
      throw new IllegalArgumentException("No matching JSON schema definition at: " + pointer + ", suffix: " + suffix);
    }
    SchemaValidator schemaValidator = new SchemaValidator(validationContext, null, schemaNode);
    return new OpenApiSchemaValidator(suffix, schemaValidator);
  }

  /**
   * Find a path definition in OAS3 spec that ends with the given suffix extension.
   * @param suffix Suffix
   * @return Path node or null
   */
  private @Nullable JsonNode findMatchingPathNode(@NotNull String suffix) {
    Pattern endsWithSuffixPattern = Pattern.compile("^.+/" + Pattern.quote(suffix) + ".json$");
    JsonNode paths = rootNode.findValue("paths");
    if (paths != null) {
      Iterator<String> fieldNames = paths.fieldNames();
      while (fieldNames.hasNext()) {
        String path = fieldNames.next();
        if (endsWithSuffixPattern.matcher(path).matches()) {
          return paths.findValue(path);
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return this.url.toString();
  }

}
