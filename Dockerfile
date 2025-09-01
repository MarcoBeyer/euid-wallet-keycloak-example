# Multi-stage build for OpenID4VP Keycloak provider
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src/

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Create the final Keycloak image
FROM quay.io/keycloak/keycloak:22.0.5

# Copy the built provider JAR from the builder stage
COPY --from=builder /app/target/openid4vp-keycloak-provider-1.0.0.jar /opt/keycloak/providers/

# Copy realm configuration for import
COPY examples/config/realm-config.json /opt/keycloak/data/import/

# Set user to root temporarily for build
USER root

# Build Keycloak with the OpenID4VP provider
RUN /opt/keycloak/bin/kc.sh build

# Switch back to keycloak user for security
USER keycloak

# Configure Keycloak
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Expose the default Keycloak port
EXPOSE 8080

# Start Keycloak with realm import
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--import-realm"]