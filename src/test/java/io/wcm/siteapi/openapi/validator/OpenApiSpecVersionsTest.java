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

import static io.wcm.siteapi.openapi.validator.OpenApiSpecVersions.DEFAULT_FILENAME_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class OpenApiSpecVersionsTest {

  @Test
  void testGetAllVersions() {
    OpenApiSpecVersions underTest = new OpenApiSpecVersions();
    assertEquals(List.of("", "v1", "v2"), List.copyOf(underTest.getAllVersions()));
    assertEquals("v2", underTest.getLatestVersion());
  }

  @Test
  void testGetValid() {
    OpenApiSpecVersions underTest = new OpenApiSpecVersions();
    OpenApiSpec spec = underTest.get("v1");
    assertEquals("v1", spec.getVersion());
  }

  @Test
  void testGetInvalid() {
    OpenApiSpecVersions underTest = new OpenApiSpecVersions();
    assertThrows(IllegalArgumentException.class, () -> {
      underTest.get("v999");
    });
  }

  @Test
  void testNoFilesFound() {
    assertThrows(IllegalArgumentException.class, () -> {
      new OpenApiSpecVersions("non-existing-path", DEFAULT_FILENAME_PATTERN, null);
    });
  }

}
