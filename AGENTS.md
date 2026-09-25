# Repository guidance

## Scope and orientation

These instructions apply throughout this repository. Read `README.md` and the relevant implementation before making changes. This is a Java 21 / Spring Boot 3.4.5 Maven application backed by SQL Server, with Conlog SOAP integration and bundled compiled React applications.

The base package is `com.jedco.jedcoinspectionspring`. Follow the existing separation:

- `controllers`: HTTP binding and response handling.
- `services`: business logic, generally an interface plus an `Impl` class.
- `repositories` and `models`: JPA persistence and domain mappings.
- `rest`: request and response DTOs.
- `mappers`: MapStruct mappings using the Spring component model.
- `configuration`: framework wiring, security, file storage, and global exception handling.
- `client` and `common`: Conlog calls and shared request construction.

## Implementation conventions

- Match nearby Java formatting and naming; keep changes focused and avoid unrelated formatting churn.
- Prefer constructor injection, following existing `final` fields and Lombok `@RequiredArgsConstructor` patterns.
- Keep business rules in services and persistence access in repositories. Reuse existing DTOs and mappers where appropriate.
- Preserve existing HTTP paths, casing, request/response field names, status values, and database mappings unless the task explicitly changes their contracts. The bundled clients depend on these contracts.
- Use the existing exception and response-handling conventions; inspect `GlobalExceptionHandler` and neighboring endpoints before adding a new error shape.
- Keep Lombok/MapStruct annotation processing and Hibernate enhancement intact. Do not edit generated output in `target/`.
- Treat `conlog/` as generated JAXB bindings. Prefer changing integration logic in `client/`, `common/`, or services. If a schema change requires regeneration, identify the authoritative schema and tooling first; the POM does not define a binding-generation workflow.
- Treat `src/main/resources/static/` as compiled frontend output. Do not hand-edit minified bundles or source maps for ordinary backend work. Frontend changes require the owning source project and its build artifacts.

## Configuration and integrations

- Keep credentials, tokens, private keys, and environment-specific connection details out of commits, logs, examples, and task reports. Use the existing environment placeholders and external configuration.
- Do not replace or modify the bundled `.p12` keystore as part of unrelated work.
- Hibernate validates an existing SQL Server schema; no migration or seed scripts are supplied. Do not switch `ddl-auto` to `create` or `update` to make a failing run pass. Explain any schema changes required by entity changes.
- Use a development database and sandbox integration credentials for runtime checks. Do not invoke live Conlog operations or mutate shared data merely to verify a build.
- Default uploads and logging target `/opt/inspection` and `/var/log/inspection`. Use external local overrides as described in `README.md` instead of changing deployment defaults or requiring privileged filesystem access.
- Security currently permits all paths at the HTTP matcher level while enabling JWT filtering and method security. Evaluate the actual filter and method annotations when changing authorization; do not assume a route is protected because Spring Security is installed.
- Some existing GET handlers mutate state. Check handler behavior before calling endpoints during validation.

## Validation

Use the checked-in Maven wrapper with JDK 21:

```bash
./mvnw -DskipTests package
./mvnw -DargLine=--enable-preview test
```

The first command checks compilation, annotation processing, enhancement, and packaging without executing tests. The second runs the tests, including the existing application-context test, which needs environment configuration, a compatible SQL Server schema, and writable or overridden logging paths. No isolated test profile or embedded database is supplied. Do not present skipped tests as passing tests.

For behavior changes, add focused tests that exercise the affected rules or contracts. Mock external services for unit tests; use an explicitly configured development environment for integration checks. Account for SQL Server semantics when testing repository queries. Documentation-only changes need content/path review and `git diff --check`, not an application startup.

Before finishing, inspect the diff for unintended changes and secrets. Report what changed, what validation actually ran, and any unmet runtime prerequisites. Update the README when setup, configuration, routes, or build instructions change.
