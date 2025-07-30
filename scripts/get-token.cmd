@echo off
REM Script para obtener tokens JWT de Keycloak para testing en Windows
REM Uso: get-token.cmd [admin|teacher|student]

setlocal enabledelayedexpansion

set KEYCLOAK_URL=http://localhost:8080
set REALM=grade-management
set CLIENT_ID=grade-management-client

if "%1"=="" (
    echo Uso: %0 [admin^|teacher^|student]
    echo Ejemplo: %0 teacher
    exit /b 1
)

set ROLE=%1

if "%ROLE%"=="admin" (
    set USERNAME=admin@test.com
    set PASSWORD=admin123
) else if "%ROLE%"=="teacher" (
    set USERNAME=teacher@test.com
    set PASSWORD=teacher123
) else if "%ROLE%"=="student" (
    set USERNAME=student@test.com
    set PASSWORD=student123
) else (
    echo Rol invalido: %ROLE%
    echo Roles validos: admin, teacher, student
    exit /b 1
)

echo Obteniendo token para usuario: %USERNAME% (rol: %ROLE%)

curl -s -X POST "%KEYCLOAK_URL%/realms/%REALM%/protocol/openid-connect/token" ^
    -H "Content-Type: application/x-www-form-urlencoded" ^
    -d "grant_type=password" ^
    -d "client_id=%CLIENT_ID%" ^
    -d "username=%USERNAME%" ^
    -d "password=%PASSWORD%" ^
    -o token_response.json

echo.
echo Respuesta guardada en token_response.json
echo.
echo Para extraer el token, usa:
echo findstr "access_token" token_response.json
echo.
echo Para usar en curl:
echo curl -H "Authorization: Bearer YOUR_TOKEN_HERE" http://localhost:8081/api/v1/grades

endlocal
