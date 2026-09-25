# JEDCO Inspection

Spring Boot application for electricity inspection workflows, including customer and meter lookup, inspection assignments, load assessments, sales and legal follow-up, quotations, file attachments, Excel exports, and user/role management. It serves REST endpoints and bundles compiled React interfaces for inspection, sales, and legal users.

## Technology

- Java 21 and Spring Boot 3.4.5
- Maven wrapper (Maven 3.9.5)
- Spring MVC, Spring Data JPA, Hibernate, and Microsoft SQL Server
- Spring Security, JWT (JJWT), and BCrypt
- Spring Web Services and JAXB for Conlog SOAP integration
- MapStruct and Lombok annotation processors
- Apache POI for Excel exports; springdoc OpenAPI for API documentation

## Project layout

Application code is under `src/main/java/com/jedco/jedcoinspectionspring/`:

| Directory | Purpose |
| --- | --- |
| `controllers/` | HTTP endpoints and React entry routes |
| `services/` | Business service interfaces and implementations |
| `repositories/` | Spring Data JPA persistence |
| `models/` | Persistence entities and domain models |
| `rest/` | Request and response DTOs |
| `mappers/` | MapStruct entity/DTO mappings |
| `configuration/` | Security, SOAP, MVC, file storage, and exception handling |
| `client/`, `common/` | Conlog client and shared request/domain helpers |
| `conlog/` | Generated JAXB XMLVend/Conlog classes |
| `exceptions/`, `Util/` | Application exceptions and utility code |

`src/main/resources/` contains application and logging configuration, a bundled TLS keystore, and compiled frontend assets under `static/inspection`, `static/inspection_sales`, and `static/inspection_legal`. There is no frontend source build or `package.json` in this repository.

## Prerequisites

Use JDK 21 with `JAVA_HOME` configured. The wrapper downloads Maven and dependencies on its first invocation. Windows users can substitute `mvnw.cmd` for `./mvnw`.

Running the application requires a SQL Server database with the existing application schema and appropriate reference/user data. Hibernate uses `ddl-auto=validate`; it does not create the schema. No database migration scripts or seed dataset are included. Obtain a development database and Conlog integration settings from the project maintainer.

## Configuration

The following environment variables are referenced by `src/main/resources/application.properties`:

| Variable | Purpose |
| --- | --- |
| `INSPECTION_DB_URL` | SQL Server JDBC URL |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `CONLOG_OPNAME`, `CONLOG_PASSWORD` | Conlog operator credentials |
| `CONLOG_DEVICE_ID`, `CONLOG_TERMINAL_ID`, `CONLOG_SERVER_ID` | Conlog client, terminal, and server identifiers |
| `CONLOG_WSDL_URL` | SOAP service URI passed to the web service client as its default endpoint |
| `INSPECTION_JWT_KEY` | Base64-encoded signing key, at least 32 decoded bytes for HS256 |
| `SSL_KEY_STORE_PASSWORD` | Keystore password when using the configured HTTPS setup |

Supply these through your shell, IDE run configuration, or deployment secret management. Spring Boot does not automatically load a `.env` file. Do not commit credentials or local configuration containing secrets.

The checked-in defaults use HTTPS on port `8085`, uploads in `/opt/inspection`, and rolling logs in `/var/log/inspection/info` and `/var/log/inspection/error` with 30-day retention. Multipart file and request limits are both 10 MB. Access tokens expire after 20 days and refresh tokens after 7 days.

### Local development

After supplying the environment variables above, use a development database and override deployment-specific settings. For example, create `/tmp/jedco-logback-local.xml` with console logging:

```xml
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>
</configuration>
```

Then run:

```bash
mkdir -p /tmp/jedco-inspection-uploads
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="--enable-preview" \
  -Dspring-boot.run.arguments="--server.address=127.0.0.1 --server.ssl.enabled=false --server.port=8085 --file.upload-dir=/tmp/jedco-inspection-uploads --logging.config=file:/tmp/jedco-logback-local.xml"
```

This disables TLS for the local process and avoids requiring access to the deployment upload/log directories. The custom `logback-spring.xml` hardcodes its log paths, so changing `logging.file.path` alone does not redirect those appenders.

The controller entry routes are `/inspection`, `/inspection_sales`, and `/inspection_legal`. OpenAPI JSON is configured at `/api`; Swagger UI is available at `/swagger-ui/index.html`.

## Build and test

```bash
# Compile and package without running the database-backed context test
./mvnw -DskipTests package

# Run tests with the development environment configured
./mvnw -DargLine=--enable-preview test

# Run the packaged application with the same environment/local overrides as needed
java --enable-preview -jar target/jedco-inspection-spring-0.0.1-SNAPSHOT.jar
```

The compiler enables Java 21 preview features, so the examples include the runtime flag. The current test suite contains a single `@SpringBootTest` context-load test, with no isolated test profile or embedded database. It requires the application configuration, a compatible database, and usable logging paths (or an external logging configuration). Packaging with tests skipped does not verify startup or integrations.

## API and security behavior

Main endpoint groups include `/auth`, `/inspections`, `/assessment`, `/sales`, `/legal`, `/customerService`, `/conlogService`, `/inspectionFiles`, and `/taskHistory`, plus user, role, and reference-data endpoints. Route names are case-sensitive; some existing routes use mixed casing such as `/User` and `/UserRole`.

Authentication endpoints are `POST /auth/login` and `POST /auth/refresh`. JWT filtering and method security are configured, but the current HTTP security matcher permits all paths (`/**`). Inspect method-level authorization and the JWT filter before assuming an endpoint requires authentication. Several existing GET endpoints change state; do not use indiscriminate endpoint crawling for smoke tests.
