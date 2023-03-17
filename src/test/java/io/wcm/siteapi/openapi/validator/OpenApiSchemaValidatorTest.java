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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenApiSchemaValidatorTest {

  private OpenApiSchemaValidator validator;

  @BeforeEach
  void setUp() throws Exception {
    OpenApiSpec spec = new OpenApiSpecVersions().getLatest();
    validator = spec.getSchemaValidator("index");
  }

  @Test
  void validateValidJson() throws ContentValidationException {
    String json = getJson("valid.json");
    validator.validate(json);
  }

  @Test
  void validateInvalidJson() {
    String json = getJson("invalid.json");
    assertThrows(ContentValidationException.class, () -> {
      validator.validate(json);
    });
  }

  @Test
  void validateIllegalJson() {
    String json = getJson("illegal.json");
    assertThrows(ContentValidationException.class, () -> {
      validator.validate(json);
    });
  }

  private String getJson(String file) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(file)) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
