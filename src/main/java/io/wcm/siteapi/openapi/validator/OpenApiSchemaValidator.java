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

import org.jetbrains.annotations.NotNull;
import org.openapi4j.core.util.TreeUtil;
import org.openapi4j.core.validation.ValidationResults.ValidationItem;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Validator based on JSON schema for a path definition matching the given suffix.
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
   * Validates JSON content against the schema of the operation for the content generator.
   * @param jsonContent JSON content to validate
   * @throws ContentValidationException If JSON content validation fails
   */
  public void validate(@NotNull String jsonContent) throws ContentValidationException {
    try {
      JsonNode contentNode = TreeUtil.json.readTree(jsonContent);
      ValidationData<Void> validation = new ValidationData<>();
      schemaValidator.validate(contentNode, validation);
      if (!validation.isValid()) {
        // put all validation errors in a single message
        StringBuilder result = new StringBuilder();
        result.append("JSON content not valid for suffix '" + suffix + "':");
        for (ValidationItem item : validation.results().items()) {
          result.append('\n').append(item);
        }
        throw new ContentValidationException(result.toString());
      }
    }
    catch (JsonProcessingException ex) {
      throw new ContentValidationException("JSON content is invalid:\n" + jsonContent, ex);
    }
  }

}
