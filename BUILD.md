# Build Instructions

This document describes how to build the OpenID4VP Keycloak provider.

## Docker Build (Recommended)

The easiest way to build the project is using Docker, which handles all dependencies automatically:

```bash
# Using Docker Compose (builds everything)
./start.sh

# Or build just the Keycloak image manually
docker build -t openid4vp-keycloak .
```

### Docker Build Process

The Dockerfile uses a multi-stage build:

1. **Stage 1 (builder)**: Uses Maven with OpenJDK 17 to compile the Java application
   - Downloads all Maven dependencies
   - Compiles the source code
   - Creates the shaded JAR with all required dependencies

2. **Stage 2 (runtime)**: Uses the official Keycloak image
   - Copies the built JAR from stage 1
   - Installs the provider into Keycloak
   - Configures Keycloak for the OpenID4VP provider

### Benefits of Docker Build

- **Self-contained**: No need to install Maven or Java on your system
- **Consistent environment**: Same build environment every time
- **Dependency management**: All Maven dependencies handled automatically
- **Production ready**: Creates optimized final image

## Manual Build (Development)

For development, you can build manually if you have Maven and Java installed:

### Prerequisites

- Java 11 or higher
- Maven 3.8 or higher

### Build Steps

```bash
# Clean and build
mvn clean package

# The JAR will be created at:
# target/openid4vp-keycloak-provider-1.0.0.jar
```

### Install to Keycloak

```bash
# Copy to Keycloak providers directory
cp target/openid4vp-keycloak-provider-1.0.0.jar $KEYCLOAK_HOME/providers/

# Rebuild Keycloak
$KEYCLOAK_HOME/bin/kc.sh build

# Start Keycloak
$KEYCLOAK_HOME/bin/kc.sh start-dev
```

## Testing the Build

After building, you can test the provider:

1. **Docker Compose Testing**: Use `./start.sh` which includes a mock wallet
2. **Manual Testing**: Import the realm configuration from `examples/config/realm-config.json`
3. **Integration Testing**: Run the JUnit tests with `mvn test`

## Troubleshooting

### Docker Build Issues

If the Docker build fails due to network issues:

1. **Build manually first**: Run `mvn clean package` locally
2. **Use Docker**: The Dockerfile will use your pre-built JAR
3. **Network issues**: Some environments may block Maven Central access

### Maven Build Issues

Common issues and solutions:

- **Java version**: Ensure Java 11+ is installed
- **Memory issues**: Increase Maven memory with `export MAVEN_OPTS="-Xmx1024m"`
- **Dependency conflicts**: Run `mvn dependency:tree` to diagnose

## Configuration

The build process can be customized:

### Maven Properties

```bash
# Skip tests during build
mvn clean package -DskipTests

# Debug build
mvn clean package -X

# Offline build (if dependencies are cached)
mvn clean package -o
```

### Docker Build Arguments

```bash
# Build with specific Maven version
docker build --build-arg MAVEN_VERSION=3.9.6 .

# Build with different Java version
docker build --build-arg JAVA_VERSION=17 .
```

## Performance Tips

- **Layer caching**: The Dockerfile is optimized for Docker layer caching
- **Dependency caching**: Dependencies are downloaded in a separate layer
- **Multi-stage builds**: Only the final JAR is included in the runtime image
- **Parallel builds**: Use `mvn -T 1C` for parallel compilation

For more details, see [DOCKER.md](DOCKER.md) for Docker-specific instructions.