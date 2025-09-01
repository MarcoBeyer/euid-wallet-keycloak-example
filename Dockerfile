# Custom Keycloak with OpenID4VP provider
FROM quay.io/keycloak/keycloak:22.0.5

# Copy the pre-built provider JAR to Keycloak providers directory
# Build the JAR first with: mvn clean package
COPY target/openid4vp-keycloak-provider-1.0.0.jar /opt/keycloak/providers/

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