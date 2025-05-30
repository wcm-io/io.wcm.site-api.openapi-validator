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

import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.openapi4j.core.util.TreeUtil;
import org.openapi4j.core.validation.ValidationResults.ValidationItem;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Validates JSON response for a given path definition and suffix against the JSON.
 * Create instance via {@link OpenApiSpec} class.
 */
public final class OpenApiSchemaValidator {

  private final String suffix;
  private final SchemaValidator schemaValidator;

  OpenApiSchemaValidator(@NotNull String suffix, @NotNull SchemaValidator schemaValidator) {
    this.suffix = suffix;
    this.schemaValidator = schemaValidator;
  }

  /**
   * @return Suffix
   */
  public @NotNull String getSuffix() {
    return this.suffix;
  }

  /**
   * Validate the given JSON response against the operation's JSON schema.
   * @param jsonValue JSON response
   * @throws ContentValidationException Validation failed
   */
  public void validate(@NotNull String jsonValue) throws ContentValidationException {
    JsonNode node = readJson(jsonValue);
    validateAgainstSchema(node);
  }

  private JsonNode readJson(@NotNull String jsonValue) throws ContentValidationException {
    try {
      return TreeUtil.json.readTree(jsonValue);
    }
    catch (JsonProcessingException ex) {
      throw new ContentValidationException("Unable to parse JSON:\n" + jsonValue, ex);
    }
  }

  private void validateAgainstSchema(@NotNull JsonNode node) throws ContentValidationException {
    ValidationData<Void> validation = new ValidationData<>();
    schemaValidator.validate(node, validation);
    if (validation.isValid()) {
      return;
    }
    String message = "JSON invalid for suffix '" + suffix + "': " + validation.results().items().stream()
        .map(ValidationItem::toString)
        .collect(Collectors.joining("\n"));
    throw new ContentValidationException(message);
  }

}
