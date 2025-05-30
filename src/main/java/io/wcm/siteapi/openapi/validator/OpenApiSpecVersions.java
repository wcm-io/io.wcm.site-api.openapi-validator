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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

/**
 * Get available versions of Site API specification.
 *
 * <p>
 * By default, spec files are expected in the classpath at <code>/site-api-spec</code>
 * with filenames named following this pattern:
 * </p>
 *
 * <pre>
 * site-api.yaml
 * site-api-v1.yaml
 * site-api-v2.yaml
 * ...
 * </pre>
 *
 * <p>
 * But you can also specify a custom path and file name pattern.
 * </p>
 *
 * <p>
 * The spec versions are derived from the file names. If no version is detected in the filename (e.g.
 * <code>site-api.yaml</code> an empty string is used as versions. Otherwise the version from the file name
 * is returned (e.g. <code>site-api-v1.yaml</code> -&gt; <code>v1</code>). This versions reflects the
 * "major version" of the spec with expected full backward compatibility within this version.
 * </p>
 */
public final class OpenApiSpecVersions {

  /**
   * Default classpath path to look for Site API spec files.
   */
  public static final String DEFAULT_RESOURCE_PATH = "site-api-spec";

  /**
   * Default pattern for Site API spec files. Last group is expected to return the actual version.
   */
  public static final Pattern DEFAULT_FILENAME_PATTERN = Pattern.compile("site-api(-(\\w+))?.yaml");

  private final SortedSet<String> versions;
  private final Map<String, URL> urls;

  /**
   * Get all Site API Specs detected in classpath matching the default path and pattern.
   */
  public OpenApiSpecVersions() {
    this(DEFAULT_RESOURCE_PATH, DEFAULT_FILENAME_PATTERN, null);
  }

  /**
   * Get all Site API Specs detected in classpath matching given path and filename pattern.
   * @param path Directory in classpath
   * @param filenamePattern File name pattern (last group is expected to return the actual version).
   * @param versionComparator Comparator for versions ("highest" version is last version) -
   *          or null to use standard string sorting
   */
  public OpenApiSpecVersions(@NotNull String path, @NotNull Pattern filenamePattern,
      @Nullable Comparator<String> versionComparator) {
    versions = new TreeSet<>(versionComparator);
    urls = new HashMap<>();
    // get all matching spec files from classpath
    try (ScanResult scanResult = new ClassGraph().acceptPathsNonRecursive(path).scan()) {
      scanResult.getAllResources().forEach(resource -> {
        String filename = FilenameUtils.getName(resource.getPath());
        Matcher matcher = filenamePattern.matcher(filename);
        if (matcher.matches()) {
          String version = StringUtils.defaultString(matcher.group(matcher.groupCount()));
          versions.add(version);
          urls.put(version, resource.getURL());
        }
      });
    }
    if (versions.isEmpty()) {
      throw new IllegalArgumentException("No Site API spec found in classpath at '" + path + "' "
          + "with pattern: " + filenamePattern);
    }
  }

  /**
   * Get all Site API versions.
   * @return Versions
   */
  public @NotNull Collection<String> getAllVersions() {
    return Collections.unmodifiableCollection(versions);
  }

  /**
   * Get latest version.
   * @return Version
   */
  public @NotNull String getLatestVersion() {
    return versions.last();
  }

  /**
   * Returns Site API specification for highest version number.
   * @return Site API specification.
   */
  public @NotNull OpenApiSpec getLatest() {
    return get(getLatestVersion());
  }

  /**
   * Returns Site API specification.
   * @param version Requested spec version
   * @return Site API specification.
   */
  public @NotNull OpenApiSpec get(@NotNull String version) {
    URL url = urls.get(version);
    if (url == null) {
      throw new IllegalArgumentException("Invalid version: " + version);
    }
    return new OpenApiSpec(url, version);
  }

}
