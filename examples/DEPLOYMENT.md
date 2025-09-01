# Deployment Instructions

## Prerequisites

- Keycloak 22.0.5 or higher
- Java 11 or higher
- Maven 3.6 or higher

## Step 1: Build the Provider

```bash
# Clone the repository
git clone https://github.com/MarcoBeyer/euid-wallet-keycloak-example.git
cd euid-wallet-keycloak-example

# Build the provider
mvn clean package

# Verify the JAR was created
ls -la target/openid4vp-keycloak-provider-1.0.0.jar
```

## Step 2: Deploy to Keycloak

```bash
# Set Keycloak home directory
export KEYCLOAK_HOME=/path/to/keycloak

# Copy the provider JAR to Keycloak
cp target/openid4vp-keycloak-provider-1.0.0.jar $KEYCLOAK_HOME/providers/

# Build Keycloak with the new provider
$KEYCLOAK_HOME/bin/kc.sh build

# Verify the provider was loaded (check logs for any errors)
$KEYCLOAK_HOME/bin/kc.sh show-config
```

## Step 3: Start Keycloak

### Development Mode
```bash
$KEYCLOAK_HOME/bin/kc.sh start-dev
```

### Production Mode
```bash
# Configure database and other production settings first
$KEYCLOAK_HOME/bin/kc.sh start --optimized
```

## Step 4: Configure the Identity Provider

### Using Admin Console

1. Open Keycloak Admin Console: http://localhost:8080/admin
2. Log in with admin credentials
3. Create a new realm or select an existing one
4. Navigate to "Identity Providers" in the left menu
5. Click "Add provider"
6. Select "OpenID for Verifiable Presentations" from the dropdown
7. Configure the provider settings:

   - **Alias**: `eu-digital-wallet`
   - **Display Name**: `EU Digital Identity Wallet`
   - **Wallet Endpoint**: `https://your-wallet-endpoint.com`
   - **Trusted PID Issuers**: `did:web:issuer1.gov,did:web:issuer2.gov`
   - **Require Age Verification**: `true`

8. Save the configuration

### Using JSON Import

```bash
# Import the example realm configuration
curl -X POST \
  http://localhost:8080/admin/realms \
  -H 'Authorization: Bearer YOUR_ADMIN_TOKEN' \
  -H 'Content-Type: application/json' \
  -d @examples/config/realm-config.json
```

## Step 5: Test the Integration

1. Navigate to your realm's login page
2. You should see an "EU Digital Identity Wallet" button
3. Click the button to test the OpenID4VP flow
4. The system will redirect to the configured wallet endpoint

## Step 6: Configure Authentication Flow (Optional)

To make OpenID4VP the default authentication method:

1. Go to "Authentication" in the Keycloak admin console
2. Select the "Browser" flow
3. Add the "Identity Provider Redirector" authenticator
4. Configure it to use your OpenID4VP provider by default

## Production Considerations

### Security Configuration

1. **HTTPS Only**: Ensure all endpoints use HTTPS in production
2. **Trusted Issuers**: Configure only trusted PID issuers
3. **Token Size Limits**: Adjust maxVPTokenSize based on your needs
4. **Validation Timeouts**: Configure appropriate timeouts

### Environment Variables

```bash
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=your-secure-password
export KC_DB=postgres
export KC_DB_URL=jdbc:postgresql://localhost:5432/keycloak
export KC_DB_USERNAME=keycloak
export KC_DB_PASSWORD=your-db-password
export KC_HOSTNAME=your-keycloak-domain.com
```

### Database Configuration

For production, configure a proper database:

```bash
# PostgreSQL example
$KEYCLOAK_HOME/bin/kc.sh build --db=postgres

$KEYCLOAK_HOME/bin/kc.sh start \
  --db=postgres \
  --db-url=jdbc:postgresql://localhost:5432/keycloak \
  --db-username=keycloak \
  --db-password=your-password \
  --hostname=your-domain.com
```

### Monitoring and Logging

1. Enable event logging for audit trails
2. Monitor authentication success/failure rates
3. Set up alerts for validation errors
4. Configure log rotation for provider logs

### Performance Tuning

1. **Connection Pooling**: Configure HTTP connection pools for wallet endpoints
2. **Caching**: Enable appropriate caching for DID resolution
3. **Thread Pools**: Adjust Keycloak thread pool sizes
4. **JVM Settings**: Optimize heap size and garbage collection

## Troubleshooting

### Common Issues

1. **Provider Not Found**
   - Verify JAR is in providers/ directory
   - Check that kc.sh build was run after copying JAR
   - Review Keycloak startup logs for errors

2. **VP Validation Failures**
   - Check trusted issuer configuration
   - Verify wallet endpoint is accessible
   - Review VP token format and content

3. **User Creation Issues**
   - Check attribute mapping configuration
   - Verify required PID claims are present
   - Review user federation settings

### Log Analysis

```bash
# Check Keycloak logs
tail -f $KEYCLOAK_HOME/data/log/keycloak.log

# Filter for OpenID4VP related logs
grep "OpenID4VP" $KEYCLOAK_HOME/data/log/keycloak.log

# Check for errors
grep "ERROR" $KEYCLOAK_HOME/data/log/keycloak.log | grep -i openid4vp
```

### Debug Mode

Enable debug logging for the provider:

```xml
<!-- Add to $KEYCLOAK_HOME/conf/keycloak.conf -->
<logger category="com.example.openid4vp" level="DEBUG"/>
```

## Backup and Recovery

1. **Database Backup**: Regularly backup your Keycloak database
2. **Configuration Export**: Export realm configurations
3. **Provider Backup**: Keep copies of provider JARs and configurations

```bash
# Export realm configuration
$KEYCLOAK_HOME/bin/kc.sh export --realm your-realm --file realm-backup.json
```

## Updating the Provider

1. Build the new version
2. Stop Keycloak
3. Replace the JAR in providers/ directory
4. Run kc.sh build
5. Start Keycloak
6. Test the functionality

```bash
# Update process
$KEYCLOAK_HOME/bin/kc.sh stop
cp target/openid4vp-keycloak-provider-NEW-VERSION.jar $KEYCLOAK_HOME/providers/
rm $KEYCLOAK_HOME/providers/openid4vp-keycloak-provider-OLD-VERSION.jar
$KEYCLOAK_HOME/bin/kc.sh build
$KEYCLOAK_HOME/bin/kc.sh start
```