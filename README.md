# LibreLog - GayPHX Radio Traffic System

A professional radio traffic, scheduling, and automation system built to integrate LibreTime and AzuraCast for GayPHX Radio.

## Architecture

- **Backend**: Spring Boot (Java) with PostgreSQL
- **Database Migrations**: Liquibase
- **Build Tool**: Maven
- **Deployment**: Docker Compose on same server as LibreTime
- **Integrations**: LibreTime API, AzuraCast API, LibreTalk (future)

## Prerequisites

- Java 21 or higher (Java 17 minimum, managed via jenv)
- Maven 3.6+
- PostgreSQL 12+
- Docker and Docker Compose (for containerized deployment)

## Quick Start

### Local Development Setup

**Note:** All Maven commands should be run from the root `librelog/` directory (parent module).

1. **Navigate to the project root directory:**
   ```bash
   cd librelog
   ```

2. **Set Java Home (using jenv, if available):**
   ```bash
   export JAVA_HOME=$(jenv javahome)
   ```
   
   If jenv is not available or you encounter permission issues, you can set JAVA_HOME manually:
   ```bash
   export JAVA_HOME=/path/to/your/java/home
   # Or on macOS with Homebrew:
   export JAVA_HOME=$(/usr/libexec/java_home)
   ```

3. **Build the application:**
   ```bash
   mvn clean compile
   ```

4. **Run the application:**
   ```bash
   mvn spring-boot:run -pl librelog-web-api
   ```

5. **Access the application:**
   - **API**: http://localhost:8080
   - **API Docs**: http://localhost:8080/swagger-ui.html (if Swagger is configured)
   - **Actuator Health**: http://localhost:8080/actuator/health

### Docker Deployment

1. **Copy environment template:**
   ```bash
   cp env.template .env
   ```

2. **Configure API keys in `.env`:**
   ```bash
   # Edit .env with your LibreTime and AzuraCast API keys
   LIBRETIME_URL=https://your-libretime-url/api
   LIBRETIME_API_KEY=your-libretime-api-key
   AZURACAST_URL=https://your-azuracast-url/api
   AZURACAST_API_KEY=your-azuracast-api-key
   
   # Database configuration
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/librelog
   SPRING_DATASOURCE_USERNAME=librelog
   SPRING_DATASOURCE_PASSWORD=your-password
   ```

3. **Start the system:**
   ```bash
   docker-compose up -d
   ```

4. **Access the application:**
   - **API**: http://localhost:8080
   - **API Docs**: http://localhost:8080/swagger-ui.html
   - **Setup Page**: http://localhost:8080/setup

5. **Initial Setup:**
   - Go to http://localhost:8080/setup
   - Create your admin user
   - Verify API key configuration
   - Login at http://localhost:8080/login

## Database Migrations

This project uses **Liquibase** for database schema management. Migrations are located in the `librelog-liquibase` module at `librelog-liquibase/src/main/resources/db/changelog/`.

### Running Migrations

Migrations run automatically on application startup. To manually update the database:

```bash
# From the web-api module
cd librelog-web-api
mvn liquibase:update

# Or from root, targeting the web-api module
mvn liquibase:update -pl librelog-web-api
```

### Creating New Migrations

1. Create a new changelog file in `librelog-liquibase/src/main/resources/db/changelog/`
2. Follow the naming convention: `{number}-{description}.xml` (e.g., `001-create-users-table.xml`)
3. Add it to `db.changelog-master.xml` using the `<include>` tag
4. **Important**: Once a changeset has been applied to a database, it must **never** be modified. Always create new changesets for additional changes.
5. Run the application or execute `mvn liquibase:update` to apply migrations

## Development

### Building the Project

**Note:** All commands should be run from the root `librelog/` directory (parent module).

```bash
# Set Java Home (if using jenv)
export JAVA_HOME=$(jenv javahome)

# Clean and compile all modules
mvn clean compile

# Run tests for all modules (requires Docker for Testcontainers)
mvn test

# Skip tests during build (if Docker is not available)
mvn clean install -DskipTests

# Package all modules
mvn clean package

# Run the web-api application
cd librelog-web-api
mvn spring-boot:run
# Or from root:
mvn spring-boot:run -pl librelog-web-api
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TestLibreLogApplication

# Run with coverage
mvn test jacoco:report
```

### Application Configuration

Configuration is managed through `application.properties` or `application.yml` in `src/main/resources/`. Environment-specific configurations can be overridden using Spring profiles.

Example profiles:
- `application-dev.properties` - Development environment
- `application-prod.properties` - Production environment

Run with a specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Default Login Credentials

After running initial setup:
- **Username**: `admin`
- **Password**: `admin123`

**⚠️ Change this password immediately after first login!**

## Features (Alpha)

- ✅ Music library management with metadata tagging
- ✅ Clock template builder for radio scheduling
- ✅ Campaign and PSA management with ad fallback
- ✅ Daily log generation and LibreTime integration
- ✅ Voice tracking with web recorder
- ✅ Playback reconciliation and compliance reporting
- ✅ AzuraCast metadata sync

## Project Structure

This is a multi-module Maven project with the following structure:

```
librelog/                          # Parent module
├── pom.xml                        # Parent POM
├── librelog-liquibase/            # Liquibase database migration module
│   ├── pom.xml
│   └── src/main/resources/
│       └── db/changelog/
│           └── db.changelog-master.xml
└── librelog-web-api/              # Spring Boot REST API module
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/onelpro/librelog/
        │   │   ├── config/          # Configuration classes
        │   │   ├── controllers/     # REST controllers
        │   │   ├── services/        # Service interfaces
        │   │   │   └── impl/        # Service implementations
        │   │   ├── repositories/    # Data access layer
        │   │   ├── models/          # Entity models (JPA entities)
        │   │   ├── dto/             # Data transfer objects
        │   │   ├── enums/           # Enum classes
        │   │   ├── exceptions/      # Custom exceptions
        │   │   ├── utils/           # Utility/helper classes
        │   │   └── LibreLogApplication.java
        │   └── resources/
        │       └── application.properties
        └── test/
            └── java/com/onelpro/librelog/
                ├── controllers/      # Controller tests
                ├── enums/            # Enum tests
                ├── exceptions/       # Exception tests
                ├── repositories/     # Repository tests
                ├── services/         # Service tests
                │   └── impl/        # Service implementation tests
                └── integration/      # Integration tests
```

### Module Descriptions

- **librelog-liquibase**: Contains all Liquibase database migration changesets. This module is included as a dependency in the web-api module to provide database schema management.

- **librelog-web-api**: The main Spring Boot application module containing all REST controllers, services, models, DTOs, and business logic.

## Testing

### Test Database Strategy

The project uses two test database approaches:

1. **H2 In-Memory Database** (`LibreLogApplicationTests`)
   - Fast, lightweight tests for basic context loading
   - No Docker required
   - Suitable for tests that don't require PostgreSQL-specific features

2. **PostgreSQL via Testcontainers** (`LibreLogApplicationPostgresTests`)
   - Full PostgreSQL container for integration tests
   - Required for tests using PostgreSQL-specific features (e.g., JSON column types)
   - Requires Docker to be running

### Running Tests

```bash
# Run all tests (H2 tests will run; PostgreSQL tests require Docker)
mvn test

# Run only H2-based tests (fast, no Docker required)
mvn test -Dtest=LibreLogApplicationTests

# Run only PostgreSQL-based tests (requires Docker)
mvn test -Dtest=LibreLogApplicationPostgresTests

# Skip tests during build
mvn clean install -DskipTests
```

### Writing Tests

- **For basic tests**: Extend or use `LibreLogApplicationTests` as a base
- **For tests requiring JSON columns or other PostgreSQL features**: Extend or use `LibreLogApplicationPostgresTests` and import `TestcontainersConfiguration`
- **Test Structure**: Tests mirror the main package structure:
  - `controllers/` - Controller tests (e.g., `UserControllerTest.java`)
  - `enums/` - Enum tests (e.g., `UserStatusTest.java`)
  - `exceptions/` - Exception handling tests
  - `repositories/` - Repository tests (use `@DataJpaTest`)
  - `services/` - Service interface tests
  - `services/impl/` - Service implementation tests
  - `integration/` - End-to-end integration tests (suffix "IT", e.g., `UserIT.java`)
- **Note**: DTO and Model classes do not require unit tests

### Code Coverage with JaCoCo

This project uses **JaCoCo** for code coverage analysis with the following requirements:

- ✅ **Minimum 80% code coverage** is required for all production code
- ✅ Coverage reports are generated automatically during the test phase
- ✅ Build will fail if coverage falls below 80%
- ✅ Coverage reports are available at: `librelog-web-api/target/site/jacoco/index.html`

**Running Coverage Reports:**

```bash
# Run tests and generate coverage report
mvn clean test

# View coverage report
open librelog-web-api/target/site/jacoco/index.html
```

**Coverage Exclusions:**
- DTO classes (data transfer objects with only Lombok-generated methods)
- Model/Entity classes (JPA entities with only Lombok-generated methods)
- Application main class
- Configuration classes (test infrastructure)
- Focus is on meaningful coverage: business logic, edge cases, and error handling

**Note:** DTO and Model classes do not require unit tests as they contain only data structures with Lombok-generated methods. Coverage focuses on business logic in services, controllers, and repositories.

## API Documentation

API documentation is available via:
- **Swagger UI**: http://localhost:8080/swagger-ui.html (if configured)
- **Actuator Endpoints**: http://localhost:8080/actuator

## License

Proprietary - GayPHX Radio
