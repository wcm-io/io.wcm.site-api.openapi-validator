## About Site API Open API Validator

Validates OAS3 schemas and JSON content.

[![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.site-api.openapi-validator)](https://repo1.maven.org/maven2/io/wcm/io.wcm.site-api.openapi-validator/)


### Documentation

* [Usage][usage]
* [API documentation][apidocs]
* [Changelog][changelog]


### Overview

* Validate OAS3 YAML specification files to ensure the files are valid according to the OAS3 spec and JSON schema definition
* Validate actual JSON content against the JSON schema contained in the OAS3 YAML specification files
* Allows to detect and read multiple versions of OAS3 YAML specification files in the class path

This module can be used in AEM projects based on the [Site API][site-api] to validate JSON content in integration tests (Custom Functional Tests) and unit tests. The module is not intended to be deployed to AEM.

### GitHub Repository

Sources: https://github.com/wcm-io/io.wcm.site-api.openapi-validator


[usage]: usage.html
[apidocs]: apidocs/
[changelog]: changes.html
[site-api]: https://wcm.io/site-api/
