# Built-in keyboard dependencies

## Moshi 1.15.2

- Scope: runtime, `com.squareup.moshi:moshi:1.15.2`.
- Purpose: strict typed JSON binding with `failOnUnknown()`; semantic validation remains project-local.
- License: Apache License 2.0.
- Compatibility: Java 8 bytecode and Android API 21; the keyboard DTO package has narrow R8 keep rules because it is decoded reflectively.
- Maintenance/size: pinned stable 1.15.x core only; no Kotlin runtime or adapter framework is included.

## NetworkNT JSON Schema Validator 1.0.87

- Scope: JVM tests only, `com.networknt:json-schema-validator:1.0.87`.
- Purpose: Draft 2020-12 conformance for the published schema and golden bundled fixture.
- License: Apache License 2.0.
- Packaging: declared with `testImplementation` and absent from application runtime dependencies/APKs.
