# OpenID4VP Keycloak Identity Provider

A Keycloak identity provider implementation for OpenID4VP (OpenID for Verifiable Presentations) with EU Digital Identity Wallet (EUID) integration. This provider enables secure authentication using Personal Identification Data (PID) credentials through verifiable presentations.

## Features

- **OpenID4VP Draft 24 Compliance**: Implements the latest OpenID4VP specification
- **eIDAS 2.0 Support**: Supports EU Digital Identity Wallet PID credentials
- **Secure Verification**: Cryptographic signature validation and replay protection
- **PID Attribute Extraction**: Extracts KYC data from verified credentials
- **Role-based Access**: Automatic role assignment based on verification status
- **Configurable Security**: Customizable security controls and validation rules

## Architecture

The implementation extends Keycloak's Service Provider Interface (SPI) architecture:

- `OpenID4VPIdentityProvider`: Main identity provider handling authentication flows
- `VPTokenValidator`: VP token verification and PID credential validation
- `SecurityConfiguration`: Security controls and nonce generation
- `PIDCredentials`: Data model for extracted PID attributes

## Quick Start with Docker

### Option 1: Docker Compose (Recommended)

The fastest way to try the OpenID4VP provider is using Docker Compose:

```bash
# Clone the repository
git clone https://github.com/MarcoBeyer/euid-wallet-keycloak-example.git
cd euid-wallet-keycloak-example

# Start the example (builds everything automatically)
./start.sh
```

This will:
- Build the OpenID4VP provider from source inside Docker
- Start Keycloak with PostgreSQL database
- Import the example realm configuration
- Start a mock wallet for testing

**Access URLs:**
- Keycloak Admin Console: http://localhost:8080/admin (admin/admin)
- Test Authentication: http://localhost:8080/realms/eudi-authentication/account
- Mock Wallet: http://localhost:8081

To stop the services:
```bash
./stop.sh
```

### Option 2: Manual Installation

### 1. Build the Provider

```bash
mvn clean package
```

### 2. Deploy to Keycloak

```bash
# Copy the built JAR to Keycloak providers directory
cp target/openid4vp-keycloak-provider-1.0.0.jar $KEYCLOAK_HOME/providers/

# Build Keycloak with the new provider
$KEYCLOAK_HOME/bin/kc.sh build

# Start Keycloak
$KEYCLOAK_HOME/bin/kc.sh start-dev
```

### 3. Configure the Identity Provider

1. Log in to Keycloak Admin Console
2. Navigate to your realm settings
3. Go to "Identity Providers"
4. Click "Add provider" and select "OpenID for Verifiable Presentations"
5. Configure the provider settings (see Configuration section)

## Configuration

### Required Settings

- **Wallet Endpoint**: The OpenID4VP endpoint URL for wallet integration
- **Trusted PID Issuers**: Comma-separated list of trusted PID issuer DIDs

### Optional Settings

- **Require Age Verification**: Require age_over_18 claim in PID (default: true)
- **Custom Presentation Definition**: Override default PID presentation definition
- **Maximum VP Token Size**: Maximum size for VP tokens in bytes (default: 102400)
- **Validation Timeout**: VP validation timeout in seconds (default: 30)

### Example Configuration

```json
{
  "alias": "eu-digital-wallet",
  "providerId": "openid4vp",
  "enabled": true,
  "config": {
    "walletEndpoint": "https://wallet.example.com",
    "trustedIssuers": "did:web:issuer1.gov,did:web:issuer2.gov",
    "requireAgeVerification": "true",
    "maxVPTokenSize": "102400",
    "validationTimeout": "30"
  }
}
```

## Authentication Flow

1. **Initiation**: User selects OpenID4VP provider for authentication
2. **Presentation Request**: Provider generates presentation definition for PID credentials
3. **Wallet Redirect**: User is redirected to wallet with authorization request
4. **VP Generation**: Wallet creates verifiable presentation with PID credentials
5. **Callback**: Wallet redirects back with VP token
6. **Validation**: Provider validates VP token and extracts PID attributes
7. **User Creation/Update**: User account is created or updated with PID data

## Presentation Definition

The provider uses a default presentation definition for EU PID credentials:

```json
{
  "id": "pid-authentication",
  "input_descriptors": [{
    "id": "eu.europa.ec.eudiw.pid.1",
    "format": {
      "vc+sd-jwt": {"sd-jwt_alg_values": ["ES256", "ES384", "EdDSA"]},
      "mso_mdoc": {"alg": ["ES256", "ES384", "ES512", "EdDSA"]}
    },
    "name": "EU Digital Identity PID",
    "purpose": "Authentication with PID credentials",
    "constraints": {
      "fields": [
        {"path": ["$.family_name"], "intent_to_retain": false},
        {"path": ["$.given_name"], "intent_to_retain": false},
        {"path": ["$.birthdate"], "intent_to_retain": false},
        {"path": ["$.age_over_18"], "intent_to_retain": false}
      ]
    }
  }]
}
```

## PID Attributes

The following PID attributes are extracted and mapped to Keycloak user attributes:

- `family_name`: User's family name
- `given_name`: User's given name
- `birthdate`: Birth date
- `age_over_18`: Age verification (boolean)
- `nationality`: Nationality
- `personal_administrative_number`: Administrative number (if available)
- `pid_issuer`: DID of the PID issuer
- `pid_valid_until`: PID credential expiration

## User Roles

The provider automatically assigns roles based on verification status:

- `pid-verified`: Assigned to all users authenticated via PID
- `adult-verified`: Assigned when age_over_18 claim is true

## Security Features

### Cryptographic Verification
- VP signature validation against DID documents
- Credential integrity verification
- Issuer validation against trusted registries

### Replay Protection
- Secure nonce generation (128-bit entropy)
- Nonce binding validation
- Request timestamp validation

### Input Validation
- VP token size limits (DoS protection)
- DID format validation
- HTTPS enforcement for wallet endpoints

### Temporal Validation
- Credential validity period checking
- Request timeout enforcement
- Clock skew tolerance

## Supported Formats

- **SD-JWT VP**: Selective Disclosure JWT Verifiable Presentations
- **JSON-LD VP**: JSON-LD Verifiable Presentations
- **mDoc**: Mobile Document format support

## Error Handling

The provider includes comprehensive error handling for:

- Invalid VP tokens
- Untrusted issuers
- Expired credentials
- Missing required claims
- Cryptographic validation failures

## Development

### Testing

Run the test suite:

```bash
mvn test
```

### Building

```bash
mvn clean package
```

### Dependencies

- Keycloak Server SPI 22.0.5
- Nimbus JOSE+JWT for JWT processing
- Jackson for JSON processing
- Apache HttpClient for HTTP operations

## Compliance

- **OpenID4VP Draft 24**: Full specification compliance
- **W3C Verifiable Credentials**: Data model compliance
- **eIDAS 2.0**: EU Digital Identity regulation compliance
- **GDPR**: Privacy by design with selective disclosure

## License

MIT License - see LICENSE file for details

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## Support

For issues and questions:
- Create an issue in the GitHub repository
- Review the Keycloak SPI documentation
- Check the OpenID4VP specification

## Related Links

- [OpenID4VP Specification](https://openid.net/specs/openid-4-verifiable-presentations-1_0-21.html)
- [Keycloak SPI Documentation](https://www.keycloak.org/docs/latest/server_development/)
- [W3C Verifiable Credentials](https://www.w3.org/TR/vc-overview/)
- [eIDAS 2.0 Regulation](https://ec.europa.eu/digital-building-blocks/sites/display/EUDIGITALIDENTITYWALLET/)