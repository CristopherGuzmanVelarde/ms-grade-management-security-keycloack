@echo off
REM Script para probar endpoints de seguridad en Windows
REM Uso: test-security.cmd

setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8081
set KEYCLOAK_URL=http://localhost:8080
set REALM=grade-management
set CLIENT_ID=grade-management-client

echo ========================================
echo Probando Seguridad del Microservicio
echo ========================================
echo.

echo 1. Probando endpoints publicos (sin token)...
echo.
echo GET /api/v1/public/health
curl -s %BASE_URL%/api/v1/public/health
echo.
echo.

echo GET /api/v1/public/info
curl -s %BASE_URL%/api/v1/public/info
echo.
echo.

echo 2. Probando endpoint protegido sin token (deberia devolver 401)...
echo.
echo GET /api/v1/grades
curl -s -w "HTTP Status: %%{http_code}\n" %BASE_URL%/api/v1/grades
echo.
echo.

echo 3. Obteniendo token de teacher...
curl -s -X POST "%KEYCLOAK_URL%/realms/%REALM%/protocol/openid-connect/token" ^
    -H "Content-Type: application/x-www-form-urlencoded" ^
    -d "grant_type=password" ^
    -d "client_id=%CLIENT_ID%" ^
    -d "username=teacher@test.com" ^
    -d "password=teacher123" ^
    -o teacher_token.json

echo Token de teacher guardado en teacher_token.json
echo.

echo 4. Probando endpoint con token de teacher...
echo.
echo Para probar manualmente:
echo 1. Extraer el access_token de teacher_token.json
echo 2. Ejecutar: curl -H "Authorization: Bearer TOKEN_AQUI" %BASE_URL%/api/v1/grades
echo.

echo ========================================
echo Pruebas completadas
echo ========================================
echo.
echo Archivos generados:
echo - teacher_token.json (contiene el JWT del teacher)
echo.
echo Endpoints para probar manualmente:
echo - GET %BASE_URL%/api/v1/grades (requiere token)
echo - POST %BASE_URL%/api/v1/grades (requiere token)
echo - DELETE %BASE_URL%/api/v1/grades/ID (solo ADMIN)

endlocal
