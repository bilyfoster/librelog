# Coding Guidelines for AI Agent (Spring Boot Java Web App)

The following rules define coding standards and best practices for generating and maintaining a Spring Boot Java web application. All generated code must strictly follow these guidelines.

---

## 1. Imports

- ❌ Do **not** use wildcard imports (e.g., `import java.util.*;`).
- ✅ Use explicit imports for clarity and maintainability.
- ✅ **Static imports** must appear **above regular imports** in the file, grouped together.

---

## 2. Logging

- ❌ Do **not** use `System.out.println` or `printStackTrace()`.
- ✅ Use **Log4j** (or `Slf4j` facade with Log4j binding) for all logging.
- Logging levels should be used appropriately:
  - `info` for normal operation
  - `warn` for recoverable issues
  - `error` for unexpected failures
  - `debug`/`trace` for detailed troubleshooting

---

## 3. Enums

- Each `enum` should be defined in its **own class file**.
- All enums must be located inside an **`enums` folder** in the main package.
- Do not define enums inside unrelated classes.

### Enum Class Template

All enums must follow this structure:

```java
package com.example.project.enums;

/**
 * Represents the possible statuses of a user.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    BANNED;
}
```

Rules for enums:

- Must include a **Javadoc comment** describing the purpose of the enum.
- Enum names must be **PascalCase**.
- Enum constants must be **UPPER_SNAKE_CASE**.
- Each enum should represent a **single concept** (avoid mixing unrelated constants).

### Using Enums in Model Classes

When using enums in JPA entity/model classes, **always** use the `@Enumerated(EnumType.STRING)` annotation to ensure enum values are stored as strings in the database rather than ordinal numbers:

```java
import jakarta.persistence.*;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    // ... other fields
}
```

**Why use `@Enumerated(EnumType.STRING)`?**
- ✅ **Database readability**: Enum values are stored as readable strings (e.g., "ACTIVE", "INACTIVE") instead of numeric ordinals (0, 1, 2).
- ✅ **Stability**: Adding or reordering enum values won't break existing database records.
- ✅ **Debugging**: Easier to query and understand database values directly.
- ❌ **Never use `EnumType.ORDINAL`**: Storing ordinals makes the database fragile to enum changes and harder to maintain.

### Sample Enum Unit Test

All enums should have a unit test verifying their values and basic behavior:

```java
package com.example.project.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserStatusTest {

  @Test
  void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
    UserStatus[] values = UserStatus.values();
    assertEquals(3, values.length, "UserStatus should have exactly 3 values");
    assertTrue(values[0] == UserStatus.ACTIVE);
    assertTrue(values[1] == UserStatus.INACTIVE);
    assertTrue(values[2] == UserStatus.BANNED);
  }

  @Test
  void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
    assertEquals(UserStatus.ACTIVE, UserStatus.valueOf("ACTIVE"));
    assertEquals(UserStatus.INACTIVE, UserStatus.valueOf("INACTIVE"));
    assertEquals(UserStatus.BANNED, UserStatus.valueOf("BANNED"));
  }
}
```

---

## 4. Constants

- Minimize static/global strings.
- Place shared constants in a dedicated `Constants` class or within the appropriate domain-specific class.
- ❌ Avoid "magic numbers" and hardcoded strings.

---

## 5. Service Layer

- All **Service classes** must implement a corresponding **interface**.
- Interfaces should define the contract, implementation should contain the business logic.
- Keep service methods **focused and cohesive**.
- **Project Structure Rules**:
  - Service **interface classes** must be located in the **root of the `services` folder**.
  - Service **implementations** must be placed in an `impl` subfolder inside the `services` folder.
  - File naming conventions:
    - Interfaces: `{Interface}.java` (e.g., `UserService.java`)
    - Implementations: `{Interface}Impl.java` (e.g., `UserServiceImpl.java`)

---

## 6. Project Folder Structure

A standard Spring Boot project must follow this structure for clarity and consistency:

```
src/main/java/com/onelpro/gayphxbackup/
│
├── config/                # Spring @Configuration classes
│
├── controllers/           # REST controllers (e.g., UserController.java)
│
├── dto/                   # Data Transfer Objects (e.g., UserResponseDTO.java, RegisterRequestDTO.java)
│
├── enums/                 # Enum classes (e.g., UserStatus.java)
│
├── exceptions/            # Custom exception classes
│
├── models/                # JPA entities / domain models
│
├── repositories/          # Spring Data JPA repositories
│
├── services/              # Service interfaces and implementations
│   ├── UserService.java   # Service interface
│   └── impl/              # Implementations of interfaces
│       └── UserServiceImpl.java
│
├── utils/                 # Utility/helper classes
│
└── Application.java       # Spring Boot main application entry point
```

### Test Folder Structure

All tests must mirror the main package structure and follow clear naming conventions (`{ClassName}Test.java` for unit tests, `{ClassName}IntegrationTest.java` for integration tests).

```
src/test/java/com/example/project/
│
├── controllers/           # Controller tests (e.g., UserControllerTest.java)
│
├── dto/                   # DTO-related tests
│
├── enums/                 # Enum-related tests
│
├── exceptions/            # Exception handling tests
│
├── models/                # Entity tests
│
├── repositories/          # Repository tests (use @DataJpaTest)
│
├── services/              # Service tests
│   ├── UserServiceTest.java
│   └── impl/              # Tests for implementations
│       └── UserServiceImplTest.java
│
└── integration/           # End-to-end or integration tests
    └── UserIntegrationTest.java
```

---

## 7. Naming & Formatting

- Use **camelCase** for variables and methods.
- Use **PascalCase** for classes, interfaces, and enums.
- Use **UPPER_SNAKE_CASE** for constants.
- Follow standard Java formatting (4 spaces indentation, braces on the same line).
- Class names should clearly represent their role (e.g., `UserServiceImpl`, `UserRepository`).

### DTO Naming Convention

- ✅ **All DTO classes must end with "DTO" suffix** (e.g., `UserResponseDTO.java`, `RegisterRequestDTO.java`, `AuthResponseDTO.java`).
- ✅ Use descriptive names that indicate the purpose: `{Purpose}{Type}DTO` (e.g., `RegisterRequestDTO`, `UserResponseDTO`, `LoginRequestDTO`).
- ❌ Do **not** create DTOs without the "DTO" suffix (e.g., avoid `UserResponse.java`, `RegisterRequest.java`).

---

## 8. Class & Method Design

- Keep classes and methods **small and focused** (Single Responsibility Principle).
- Prefer **immutability** where possible.
- Always use **constructor injection** for dependencies in Spring components.
- ❌ Do **not** use field injection with `@Autowired` annotation.
- Constructor injection improves testability, immutability, and clarity of dependencies.

---

## 9. Lombok Usage

Lombok should be used to reduce boilerplate code in entity classes, DTOs, and other data classes.

### Required Lombok Annotations

- ✅ **`@Data`**: Use for entity classes and DTOs to generate getters, setters, `toString()`, `equals()`, and `hashCode()` methods.
- ✅ **`@Builder`**: Use for DTOs and entities to provide a fluent builder pattern for object creation.
- ✅ **`@AllArgsConstructor`**: Use when you need a constructor with all fields (often combined with `@NoArgsConstructor`).
- ✅ **`@NoArgsConstructor`**: Use for JPA entities and classes that require a no-argument constructor.

### Lombok Usage Guidelines

- **Entity Classes**: Use `@Data`, `@Builder`, `@NoArgsConstructor`, and `@AllArgsConstructor` together for JPA entities.
- **DTOs**: Use `@Data` and `@Builder` for request/response DTOs.
- **Service Classes**: Do **not** use Lombok annotations on service classes (use explicit constructors for dependency injection).
- **Controllers**: Do **not** use Lombok annotations on controller classes.

### Example Entity with Lombok

```java
package com.onelpro.gayphxbackup.models;

import com.onelpro.gayphxbackup.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### Example DTO with Lombok

```java
package com.onelpro.gayphxbackup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private UUID id;
    private String email;
    private String name;
}
```

### When NOT to Use Lombok

- ❌ Do **not** use Lombok on service classes (use explicit constructors for dependency injection).
- ❌ Do **not** use Lombok on controller classes.
- ❌ Do **not** use `@Data` on classes that extend other classes (can cause issues with `equals()` and `hashCode()`).
- ❌ Do **not** use Lombok if you need custom implementations of generated methods.

---

## 10. Exception Handling
- Always handle exceptions securely and log appropriately.
- Never expose internal details in API responses.
- Use custom exceptions when domain-specific errors are required.
- Prefer `@ControllerAdvice` and `@ExceptionHandler` for global error handling.

---

## 11. Documentation

- Document all **public APIs** with Javadoc.
- Provide comments for **complex logic** or non-obvious design decisions.
- Ensure method signatures and class responsibilities are clear without needing to read the implementation.

---

## 12. Testing

- Write **unit tests** for all public methods.
- Use **JUnit 5** and **Mockito** (or equivalent) for testing.
- Write integration tests for critical workflows.
- Use **assertions** to validate expected outcomes.
- Aim for meaningful test names and maintainable test code.
- Test classes must follow the same package structure as the code under test.

### Code Coverage with JaCoCo

- ✅ **JaCoCo** must be used for code coverage analysis in all projects.
- ✅ **Minimum 80% code coverage** is required for all production code.
- ✅ Coverage reports must be generated during the build process (typically via Maven or Gradle).
- ✅ Coverage thresholds must be enforced in the build configuration to fail builds that fall below 80% coverage.
- ✅ Coverage reports should be reviewed as part of the code review process.
- ✅ Focus on meaningful coverage: prioritize testing business logic, edge cases, and error handling over simple getters/setters.
- ❌ Do **not** exclude classes from coverage without justification (e.g., DTOs with only Lombok-generated methods may be excluded if they contain no business logic).

### JaCoCo Configuration Example (Maven)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 13. Integration Testing Guidelines

- Use `@SpringBootTest` for full application context integration tests.
- Use `@DataJpaTest` for repository-layer tests with an embedded DB.
- Use `@WebMvcTest` for controller-layer tests (with mocked services).
- Use **Testcontainers** for external dependencies (e.g., databases, Redis, Kafka).
  - External services **must not be embedded in the codebase**.
  - Start and configure containers within the test setup.
  - Ensure containers are automatically started and stopped during tests.
  - Example: `PostgreSQLContainer`, `RedisContainer`, `KafkaContainer`.
- Always clean up test data between runs (e.g., using rollback transactions or test-specific DBs).
- Name integration test classes with the suffix **IT** (e.g., `UserIT.java`).
- Avoid depending on external systems outside the test environment — use containers instead.
- Ensure integration tests run reliably in CI/CD environments.

---

## 14. Spring Boot Best Practices

- Use **Spring Dependency Injection** instead of manual instantiation.
- Prefer **constructor injection** over field injection.
- Use **`@Service`, `@Repository`, `@Controller`** annotations where appropriate.
- Use **DTOs** for API input/output, avoid exposing entities directly.
- Externalize configuration using `application.properties` or `application.yml` (never hardcode config values).
- Secure sensitive information (e.g., passwords, API keys) via **environment variables** or **Spring Boot config management**.

---

## 15. Performance & Maintainability

- Avoid unnecessary object creation.
- Use streams and functional APIs where appropriate, but keep readability in mind.
- Ensure database queries are optimized (avoid N+1 queries).
- Keep dependencies minimal and up-to-date.

---

## 16. General Principles

- Favor **readability over cleverness**.
- Strive for **clean, maintainable, and testable** code.
- Code should follow **SOLID principles** and align with common Spring Boot architecture patterns.
- Treat warnings as errors—eliminate compiler and IDE warnings.

---

## 17. Forbidden Practices

The following practices are **strictly prohibited** and must never be used:

- ❌ Hardcoding secrets (API keys, passwords, tokens, DB credentials).
- ❌ Disabling SSL, CSRF, or other security features.
- ❌ Using deprecated APIs or libraries.
- ❌ Writing SQL directly in code (use repositories or `@Query`).
- ❌ Catching generic `Exception` without proper handling.
- ❌ Swallowing exceptions (e.g., empty catch blocks).
- ❌ Using static state for services, repositories, or entities.
- ❌ Directly exposing entities in REST controllers (must use DTOs).
- ❌ Committing test/demo code (e.g., `TODO`, temporary mocks) into production.
- ❌ Copy-pasting duplicate code instead of refactoring.

---

## 18. Security Best Practices

- Use **UUIDs** for primary keys in database entities to prevent walkable IDs that could expose internal system structure or data enumeration.
- Avoid using sequential integer IDs in REST APIs as they can be easily guessed and exploited for unauthorized access.
- When exposing entity identifiers in API responses, ensure they are UUIDs rather than auto-incrementing integers.
- UUIDs help prevent enumeration attacks and make it harder for malicious users to discover or access resources by guessing IDs.

---

## 19. Web UI/UX and Accessibility Requirements

All web pages and user interfaces must be designed with mobile responsiveness and accessibility in mind.

### Mobile Responsiveness

- ✅ **All web pages must be fully responsive** and work seamlessly across all device sizes (mobile phones, tablets, desktops).
- ✅ Use **responsive design principles**:
  - Implement fluid layouts using CSS Grid, Flexbox, or responsive frameworks (e.g., Bootstrap, Tailwind CSS).
  - Use relative units (rem, em, %) instead of fixed pixel values where appropriate.
  - Implement responsive images that scale appropriately.
  - Test on multiple screen sizes and orientations.
- ✅ **Mobile-first approach**: Design for mobile devices first, then enhance for larger screens.
- ✅ **Touch-friendly interfaces**: Ensure interactive elements (buttons, links, form inputs) are large enough for touch interaction (minimum 44x44 pixels).
- ✅ **Viewport meta tag**: Always include proper viewport meta tag in HTML:
  ```html
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  ```
- ✅ **Responsive navigation**: Implement mobile-friendly navigation (e.g., hamburger menus for mobile, full navigation for desktop).
- ❌ Do **not** create fixed-width layouts that break on smaller screens.
- ❌ Do **not** use horizontal scrolling on mobile devices unless absolutely necessary.

### Accessibility for Visually Impaired Users

- ✅ **WCAG 2.1 Level AA compliance**: All web pages must meet WCAG 2.1 Level AA accessibility standards as a minimum requirement.
- ✅ **Semantic HTML**: Use proper HTML5 semantic elements (`<header>`, `<nav>`, `<main>`, `<article>`, `<section>`, `<footer>`, etc.) to provide structure and meaning.
- ✅ **ARIA attributes**: Use ARIA (Accessible Rich Internet Applications) attributes appropriately:
  - `aria-label` for elements that need descriptive labels
  - `aria-labelledby` to reference existing labels
  - `aria-describedby` for additional descriptions
  - `aria-hidden` to hide decorative elements from screen readers
  - `role` attributes when semantic HTML is insufficient
- ✅ **Alt text for images**: All images must have descriptive `alt` attributes:
  - Decorative images: Use empty alt text (`alt=""`) or `aria-hidden="true"`
  - Informative images: Provide meaningful descriptions
  - Functional images (icons, buttons): Describe their function
- ✅ **Keyboard navigation**: Ensure all interactive elements are keyboard accessible:
  - All functionality must be operable via keyboard (Tab, Enter, Space, Arrow keys)
  - Visible focus indicators for keyboard navigation
  - Logical tab order
  - Skip links for main content
- ✅ **Color contrast**: Maintain sufficient color contrast ratios:
  - **Normal text**: Minimum 4.5:1 contrast ratio
  - **Large text** (18pt+ or 14pt+ bold): Minimum 3:1 contrast ratio
  - **Interactive elements**: Minimum 3:1 contrast ratio
  - Do not rely solely on color to convey information
- ✅ **Form accessibility**:
  - All form inputs must have associated `<label>` elements
  - Use `aria-required` for required fields
  - Provide clear error messages with `aria-describedby` or `aria-invalid`
  - Group related form fields using `<fieldset>` and `<legend>`
- ✅ **Screen reader support**: Test with screen readers (NVDA, JAWS, VoiceOver) to ensure content is properly announced.
- ✅ **Text alternatives**: Provide text alternatives for non-text content (images, icons, charts, graphs).
- ✅ **Focus management**: Implement proper focus management for dynamic content:
  - Move focus to new content when it appears (modals, dialogs, dynamic updates)
  - Trap focus within modals/dialogs
  - Return focus to triggering element when closing modals
- ✅ **Heading hierarchy**: Use proper heading hierarchy (`<h1>` through `<h6>`) without skipping levels.
- ✅ **Link text**: Use descriptive link text that makes sense out of context (avoid "click here", "read more").
- ✅ **Error identification**: Clearly identify and describe input errors in text format.
- ❌ Do **not** use color alone to indicate status, errors, or required fields.
- ❌ Do **not** create content that flashes more than 3 times per second (can cause seizures).
- ❌ Do **not** use images of text when actual text would suffice.
- ❌ Do **not** create keyboard traps that prevent users from navigating away from elements.

### Testing and Validation

- ✅ **Automated accessibility testing**: Use tools like:
  - axe DevTools
  - WAVE (Web Accessibility Evaluation Tool)
  - Lighthouse accessibility audit
  - Pa11y
- ✅ **Manual testing**: Perform manual accessibility testing with:
  - Keyboard-only navigation
  - Screen reader testing (NVDA, JAWS, VoiceOver)
  - Browser zoom testing (up to 200%)
  - Color contrast checkers
- ✅ **Responsive testing**: Test on multiple devices and browsers:
  - Mobile devices (iOS, Android)
  - Tablets
  - Desktop browsers (Chrome, Firefox, Safari, Edge)
  - Different screen resolutions
- ✅ **User testing**: When possible, involve users with disabilities in testing to gather real-world feedback.

---

## 20. Feature Implementation Requirements

When implementing any new feature, the following components **must** be created:

### REST API Implementation
- ✅ **REST Controller**: Create a controller class in the `controllers` package with proper Spring annotations (`@RestController`, `@RequestMapping`, etc.)
- ✅ **API Endpoints**: Implement all required REST endpoints using appropriate HTTP methods (GET, POST, PUT, DELETE, PATCH)
- ✅ **Request/Response DTOs**: Create DTOs in the `dto` package for all request and response payloads (must end with "DTO" suffix, e.g., `RegisterRequestDTO.java`, `UserResponseDTO.java`)
- ✅ **Swagger/OpenAPI Annotations**: Document all endpoints with `@Operation`, `@ApiResponse`, and related annotations
- ✅ **UUID-based Resources**: All resources exposed via REST APIs must use UUIDs for identification

### Web UI/UX Implementation
- ✅ **Mobile Responsive Design**: All web pages must be fully responsive and work on all device sizes
- ✅ **Accessibility Compliance**: All web pages must meet WCAG 2.1 Level AA standards
- ✅ **Semantic HTML**: Use proper HTML5 semantic elements
- ✅ **ARIA Attributes**: Implement appropriate ARIA attributes for screen reader support
- ✅ **Keyboard Navigation**: Ensure all functionality is keyboard accessible
- ✅ **Color Contrast**: Maintain minimum 4.5:1 contrast ratio for text
- ✅ **Form Accessibility**: All forms must have proper labels and error handling

### Database Changes
- ✅ **Liquibase Changeset**: Create a new XML changeset file in `gayphxbackup-liquibase/src/main/resources/db/changelog/`
- ✅ **Changeset Naming**: Use descriptive names following pattern: `{number}-{description}.xml` (e.g., `001-create-users-table.xml`)
- ✅ **Master Changelog**: Include the new changeset in `db.changelog-master.xml` using `<include>` tag
- ✅ **Idempotency**: Ensure changesets are idempotent and safe to run multiple times
- ✅ **UUID Primary Keys**: Use UUID type for all primary keys in new tables
- ❌ **Never Modify Existing Changesets**: Once a changeset has been applied to a database, it must **never** be modified. Liquibase tracks checksums of applied changesets and will fail if a changeset is modified after it has been executed.
- ✅ **Always Create New Changesets**: If you need to make additional database changes, create a **new changeset** with a new sequential number (e.g., if `011-create-event-indexes.xml` exists and you need to add another index, create `012-add-event-deleted-at-index.xml`).
- ✅ **Changeset Immutability**: Treat changesets as immutable once they have been applied. This ensures database migration history remains consistent and reproducible across all environments.

### Service Layer
- ✅ **Service Interface**: Create service interface in the `services` package
- ✅ **Service Implementation**: Create implementation in `services.impl` package
- ✅ **Transaction Management**: Use `@Transactional` annotation where appropriate
- ✅ **Exception Handling**: Implement proper exception handling with custom exceptions

### Testing
- ✅ **Unit Tests**: Write unit tests for all service methods
- ✅ **Integration Tests**: Write integration tests for REST endpoints
- ✅ **Repository Tests**: Write tests for repository methods using `@DataJpaTest`
- ✅ **Code Coverage**: Ensure all new code meets the **80% minimum coverage requirement** using JaCoCo
- ✅ **Coverage Verification**: Verify coverage reports are generated and thresholds are met before merging

### Implementation Checklist
For every feature, verify:
- [ ] REST controller created with proper annotations
- [ ] Service interface and implementation created
- [ ] Entity/model classes created with JPA annotations
- [ ] Repository interface created
- [ ] DTOs created for request/response
- [ ] Liquibase changeset created and added to master changelog
- [ ] Swagger/OpenAPI annotations added to endpoints
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] Code coverage meets 80% minimum requirement (verified via JaCoCo)
- [ ] Error handling implemented
- [ ] Security/authorization checks implemented
- [ ] Logging added where appropriate
- [ ] Web pages are mobile responsive (tested on multiple devices)
- [ ] Web pages meet WCAG 2.1 Level AA accessibility standards
- [ ] Keyboard navigation tested and working
- [ ] Screen reader compatibility verified
- [ ] Color contrast ratios meet minimum requirements

---
