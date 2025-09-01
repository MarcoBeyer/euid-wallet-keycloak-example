#!/bin/bash

# =======================================================================
# EUID Wallet Keycloak Example - Stop Script
# =======================================================================

set -e

echo "🛑 Stopping EUID Wallet Keycloak Example..."
echo "=========================================="

# Stop services
if docker compose version &> /dev/null; then
    # Use new docker compose command
    docker compose down
else
    # Use legacy docker-compose command
    docker-compose down
fi

echo "✅ Services stopped successfully!"
echo ""
echo "💡 To start again, run: ./start.sh"
echo ""
echo "🗑️  To remove all data (volumes), run:"
echo "   docker-compose down --volumes"
echo ""