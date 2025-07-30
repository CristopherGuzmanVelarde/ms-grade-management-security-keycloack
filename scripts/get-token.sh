#!/bin/bash

# Script para obtener tokens JWT de Keycloak para testing
# Uso: ./get-token.sh [admin|teacher|student]

KEYCLOAK_URL="http://localhost:8080"
REALM="grade-management"
CLIENT_ID="grade-management-client"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_usage() {
    echo -e "${YELLOW}Uso: $0 [admin|teacher|student]${NC}"
    echo -e "${YELLOW}Ejemplo: $0 teacher${NC}"
    exit 1
}

get_token() {
    local username=$1
    local password=$2
    local role=$3
    
    echo -e "${YELLOW}Obteniendo token para usuario: $username (rol: $role)${NC}"
    
    TOKEN_RESPONSE=$(curl -s -X POST \
        "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=$CLIENT_ID" \
        -d "username=$username" \
        -d "password=$password")
    
    if [ $? -eq 0 ]; then
        ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')
        
        if [ "$ACCESS_TOKEN" != "null" ] && [ "$ACCESS_TOKEN" != "" ]; then
            echo -e "${GREEN}✓ Token obtenido exitosamente${NC}"
            echo -e "${GREEN}Token JWT:${NC}"
            echo "$ACCESS_TOKEN"
            echo ""
            echo -e "${YELLOW}Para usar en curl:${NC}"
            echo "curl -H \"Authorization: Bearer $ACCESS_TOKEN\" http://localhost:8081/api/v1/grades"
            echo ""
            
            # Guardar token en archivo temporal
            echo "$ACCESS_TOKEN" > "/tmp/jwt_token_$role.txt"
            echo -e "${GREEN}Token guardado en: /tmp/jwt_token_$role.txt${NC}"
        else
            echo -e "${RED}✗ Error obteniendo token${NC}"
            echo -e "${RED}Respuesta:${NC} $TOKEN_RESPONSE"
        fi
    else
        echo -e "${RED}✗ Error conectando con Keycloak${NC}"
    fi
}

# Verificar que jq esté instalado
if ! command -v jq &> /dev/null; then
    echo -e "${RED}jq no está instalado. Instalalo primero:${NC}"
    echo "sudo apt-get install jq  # Ubuntu/Debian"
    echo "brew install jq         # macOS"
    exit 1
fi

# Verificar parámetros
if [ $# -eq 0 ]; then
    print_usage
fi

ROLE=$1

case $ROLE in
    "admin")
        get_token "admin@test.com" "admin123" "admin"
        ;;
    "teacher")
        get_token "teacher@test.com" "teacher123" "teacher"
        ;;
    "student")
        get_token "student@test.com" "student123" "student"
        ;;
    *)
        echo -e "${RED}Rol inválido: $ROLE${NC}"
        print_usage
        ;;
esac
