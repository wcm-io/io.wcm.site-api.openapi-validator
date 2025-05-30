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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class OpenApiSpecTest {

  @Test
  void testValidFile() {
    OpenApiSpec underTest = new OpenApiSpec("site-api-spec/site-api.yaml", "v123");
    assertNotNull(underTest.getURL());
    assertEquals("v123", underTest.getVersion());
    assertTrue(StringUtils.endsWith(underTest.toString(), "site-api-spec/site-api.yaml"));
  }

  @Test
  void testValidFileInvalidSpec() {
    assertThrows(SpecInvalidException.class, () -> {
      new OpenApiSpec("json-samples/invalid-spec.yaml", "");
    });
  }

  @Test
  void testInvalidFile() {
    assertThrows(IllegalArgumentException.class, () -> {
      new OpenApiSpec("json-samples/non-existing-file.yaml", "");
    });
  }

  @Test
  void testGetSchemaValidator_ValidSuffix() {
    OpenApiSpec underTest = new OpenApiSpecVersions().getLatest();
    OpenApiSchemaValidator validator = underTest.getSchemaValidator("index");
    assertNotNull(validator);
  }

  @Test
  void testGetSchemaValidator_InvalidSuffix() {
    OpenApiSpec underTest = new OpenApiSpecVersions().getLatest();
    assertThrows(IllegalArgumentException.class, () -> {
      underTest.getSchemaValidator("this-suffix-does-not-exist");
    });
  }

}
