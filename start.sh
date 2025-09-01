#!/bin/bash

# =======================================================================
# EUID Wallet Keycloak Example - Startup Script
# =======================================================================

set -e

echo "🚀 Starting EUID Wallet Keycloak Example..."
echo "========================================"

# Check if Docker and Docker Compose are installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from template..."
    cp .env.example .env
    echo "✅ .env file created. You can customize the configuration by editing .env"
fi

# Build the OpenID4VP provider first
echo "🔨 Building OpenID4VP provider..."
mvn clean package -q

if [ ! -f target/openid4vp-keycloak-provider-1.0.0.jar ]; then
    echo "❌ Failed to build OpenID4VP provider. Check Maven output above."
    exit 1
fi

echo "✅ OpenID4VP provider built successfully!"

# Build and start the services
echo "🔨 Building and starting services..."
echo "This may take a few minutes on first run..."

if docker compose version &> /dev/null; then
    # Use new docker compose command
    docker compose up --build -d
else
    # Use legacy docker-compose command
    docker-compose up --build -d
fi

echo ""
echo "⏳ Waiting for services to start..."

# Wait for PostgreSQL to be ready
echo "📦 Waiting for PostgreSQL..."
sleep 10

# Wait for Keycloak to be ready
echo "🔐 Waiting for Keycloak to start..."
echo "This can take 1-2 minutes..."

# Function to check if Keycloak is ready
check_keycloak() {
    curl -f -s http://localhost:8080/health/ready > /dev/null 2>&1
}

# Wait up to 3 minutes for Keycloak
for i in {1..36}; do
    if check_keycloak; then
        echo "✅ Keycloak is ready!"
        break
    fi
    echo "⏳ Still starting... (${i}/36)"
    sleep 5
done

if ! check_keycloak; then
    echo "❌ Keycloak failed to start within 3 minutes."
    echo "Check the logs with: docker-compose logs keycloak"
    exit 1
fi

echo ""
echo "🎉 EUID Wallet Keycloak Example is now running!"
echo "=============================================="
echo ""
echo "📋 Access URLs:"
echo "• Keycloak Admin Console: http://localhost:8080/admin"
echo "• Keycloak Login (test):  http://localhost:8080/realms/eudi-authentication/account"
echo "• Mock Wallet (testing):  http://localhost:8081"
echo ""
echo "🔑 Admin Credentials:"
echo "• Username: admin"
echo "• Password: admin"
echo ""
echo "📖 Next Steps:"
echo "1. Open Keycloak Admin Console: http://localhost:8080/admin"
echo "2. Login with admin/admin"
echo "3. Navigate to 'eudi-authentication' realm"
echo "4. Go to 'Identity Providers' to see the OpenID4VP provider"
echo "5. Test authentication at: http://localhost:8080/realms/eudi-authentication/account"
echo ""
echo "🛑 To stop the services:"
echo "   ./stop.sh"
echo ""
echo "📝 To view logs:"
echo "   docker-compose logs -f"
echo ""