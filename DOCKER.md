# Docker Quick Start Guide

This guide will help you quickly set up and test the OpenID4VP Keycloak provider using Docker.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) (version 20.10 or higher)
- [Docker Compose](https://docs.docker.com/compose/install/) (version 2.0 or higher)

## Quick Start

### 1. Clone and Start

```bash
# Clone the repository
git clone https://github.com/MarcoBeyer/euid-wallet-keycloak-example.git
cd euid-wallet-keycloak-example

# Start the complete stack
./start.sh
```

The startup script will:
- Build the OpenID4VP Keycloak provider from source inside Docker
- Start PostgreSQL database
- Start Keycloak with the provider pre-installed
- Import the example realm configuration
- Start a mock wallet for testing

### 2. Access the Services

Once started, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| Keycloak Admin Console | http://localhost:8080/admin | admin / admin |
| Test Authentication | http://localhost:8080/realms/eudi-authentication/account | - |
| Mock Wallet | http://localhost:8081 | - |

### 3. Test the OpenID4VP Flow

1. **Open the test realm**: http://localhost:8080/realms/eudi-authentication/account
2. **Click "Sign In"** - you should see an "EU Digital Identity Wallet" option
3. **Click the wallet button** - this will redirect to the mock wallet
4. **Test the flow** - the mock wallet shows what a real wallet would do

## Configuration

### Environment Variables

Copy `.env.example` to `.env` to customize the configuration:

```bash
cp .env.example .env
```

Key settings you can modify:

```bash
# Admin credentials
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# OpenID4VP settings
WALLET_ENDPOINT=https://your-wallet.com
TRUSTED_ISSUERS=did:web:issuer1.gov,did:web:issuer2.gov

# Security settings
REQUIRE_AGE_VERIFICATION=true
MAX_VP_TOKEN_SIZE=102400
VALIDATION_TIMEOUT=30
```

### Custom Realm Configuration

The realm configuration is automatically imported from `examples/config/realm-config.json`. To modify:

1. Edit the JSON file
2. Restart the services: `./stop.sh && ./start.sh`

## Advanced Usage

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f keycloak
docker-compose logs -f postgres
```

### Access Database

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U keycloak -d keycloak
```

### Rebuild Only Keycloak

```bash
# Rebuild just the Keycloak image
docker-compose build keycloak
docker-compose up -d keycloak
```

### Production Mode

For production deployment, modify `docker-compose.yml`:

```yaml
services:
  keycloak:
    environment:
      # Enable production mode
      KC_DEV_MODE: "false"
      
      # Set proper hostname
      KC_HOSTNAME: your-domain.com
      
      # Enable HTTPS
      KC_HTTPS_CERTIFICATE_FILE: /path/to/cert.pem
      KC_HTTPS_CERTIFICATE_KEY_FILE: /path/to/key.pem
      
      # Production database
      KC_DB_URL: jdbc:postgresql://prod-db:5432/keycloak
```

## Services Overview

### Keycloak

- **Image**: Custom multi-stage build based on `quay.io/keycloak/keycloak:22.0.5`
- **Port**: 8080
- **Build**: Java application built inside Docker container using Maven
- **Features**: 
  - OpenID4VP provider built and installed automatically
  - Example realm pre-configured
  - PostgreSQL persistence

### PostgreSQL

- **Image**: `postgres:15-alpine`
- **Port**: 5432 (internal only)
- **Purpose**: Persistent storage for Keycloak data

### Mock Wallet (Testing Profile)

- **Image**: `nginx:alpine`
- **Port**: 8081
- **Purpose**: Simulate an EU Digital Identity Wallet for testing

To start with the mock wallet:
```bash
docker-compose --profile testing up -d
```

## Troubleshooting

### Common Issues

**Keycloak fails to start:**
```bash
# Check logs
docker-compose logs keycloak

# Common fixes
docker-compose down --volumes  # Remove all data
./start.sh                     # Start fresh
```

**Port conflicts:**
```bash
# Change ports in docker-compose.yml
ports:
  - "8081:8080"  # Use port 8081 instead
```

**Build failures:**
```bash
# Clean rebuild
docker-compose down
docker system prune -f
./start.sh
```

### Health Checks

The services include health checks:

```bash
# Check service health
docker-compose ps

# Manual health check
curl http://localhost:8080/health/ready
```

### Data Persistence

Data is persisted in Docker volumes:
- `postgres_data`: Database data
- `keycloak_data`: Keycloak configuration and cache

To reset all data:
```bash
docker-compose down --volumes
```

## Security Considerations

### Development vs Production

This setup is configured for **development and testing**. For production:

1. **Change default passwords**
2. **Enable HTTPS**
3. **Use external database**
4. **Configure proper hostnames**
5. **Set up monitoring**
6. **Review security settings**

### Mock Wallet Warning

The included mock wallet is **only for testing**. It does not:
- Generate real verifiable presentations
- Perform cryptographic signing
- Connect to real credential issuers

For production, integrate with a real EU Digital Identity Wallet.

## Stopping and Cleanup

### Stop Services

```bash
# Stop services (keeps data)
./stop.sh

# Stop and remove everything
docker-compose down --volumes --remove-orphans
```

### Complete Cleanup

```bash
# Remove all project containers, volumes, and images
docker-compose down --volumes --rmi all --remove-orphans
```

## Next Steps

1. **Explore the Admin Console**: http://localhost:8080/admin
2. **Review the Identity Provider configuration**: Navigate to "Identity Providers" in your realm
3. **Test authentication flows**: Try the account console
4. **Customize the configuration**: Edit realm settings, mappers, and security policies
5. **Integrate with real wallets**: Replace mock wallet with actual EUID wallet endpoints

For more detailed configuration options, see the main [README.md](README.md) and [examples/DEPLOYMENT.md](examples/DEPLOYMENT.md).