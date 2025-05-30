## Site API Open API Validator usage

### Read and validate Open API specs

```java
// reads all spec files from classpath in folder /site-api-spec/site-api*.yaml
// you can provide your own paths and pattern
OpenApiSpecVersions specVersions = new OpenApiSpecVersions();

// get one of the spec versions
// the spec file itself is automatically validated at this point
OpenApiSpec spec = underTest.get("v1");
```

### Validate JSON content

```java
// gets a validator for a specific suffix
OpenApiSchemaValidator validator = spec.getSchemaValidator("index");

// validate JSON content
validator.validate(jsonString);
```
