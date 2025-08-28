# Eagle Bank

A Spring Boot banking application using OpenAPI code generation.

## Development Setup

### Prerequisites
- Java 17 (use `sdk use java 17.0.0-tem`)
- Maven 3.6+
- IntelliJ IDEA

### Initial Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd eagle-bank
   ```

2. **Set Java version**
   ```bash
   sdk use java 17.0.0-tem
   ```

3. **Generate OpenAPI sources** (Required before opening in IDE)
   ```bash
   ./mvnw generate-sources
   ```

4. **Configure IntelliJ IDEA**
   - Open the project in IntelliJ
   - Refresh Maven project: Maven tool window → Refresh icon
   - If you still see red highlighting, manually mark generated sources:
     - Right-click `target/generated-sources/openapi/src/main/java`
     - Select "Mark Directory as" → "Generated Sources Root"

### Development Workflow

#### When starting development or after OpenAPI changes:
```bash
./mvnw generate-sources
```

#### Full build and test:
```bash
./mvnw clean compile
```

#### Why this is necessary:
- The OpenAPI generator creates API interfaces and model classes from `openapi/openapi.yaml`
- Controllers implement these generated interfaces
- Without generation, IDE shows red highlighting because classes don't exist yet
- Never commit files in `target/` - they're regenerated on each build

### Project Structure
- `openapi/openapi.yaml` - API specification
- `src/main/java/org/example/controller/` - Controller implementations
- `target/generated-sources/openapi/` - Generated API and model classes (do not edit)

### Troubleshooting IDE Issues
If you see red highlighting in controllers:
1. Run `./mvnw generate-sources`
2. Refresh Maven project in IDE
3. Restart IDE if needed
