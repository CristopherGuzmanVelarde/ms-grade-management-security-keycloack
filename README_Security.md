# Segurización del Microservicio Grade Management

## 🔐 Resumen

Este documento describe la implementación de seguridad OAuth2 + JWT usando Keycloak como proveedor de identidad para el microservicio de gestión de calificaciones.

## 📋 Tabla de Contenidos

- [Arquitectura de Seguridad](#arquitectura-de-seguridad)
- [Configuración de Keycloak](#configuración-de-keycloak)
- [Configuración del Microservicio](#configuración-del-microservicio)
- [Roles y Permisos](#roles-y-permisos)
- [Endpoints de Seguridad](#endpoints-de-seguridad)
- [Configuración de Desarrollo](#configuración-de-desarrollo)
- [Configuración de Producción](#configuración-de-producción)
- [Pruebas de Seguridad](#pruebas-de-seguridad)
- [Troubleshooting](#troubleshooting)

## 🏗️ Arquitectura de Seguridad

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Keycloak     │    │  Microservicio  │
│   Application   │    │   (Auth Server) │    │  Grade Mgmt     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. Login Request      │                       │
         ├──────────────────────►│                       │
         │                       │                       │
         │ 2. JWT Token          │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ 3. API Request + JWT  │                       │
         ├───────────────────────┼──────────────────────►│
         │                       │                       │
         │                       │ 4. Validate Token    │
         │                       │◄──────────────────────┤
         │                       │                       │
         │                       │ 5. Token Claims      │
         │                       ├──────────────────────►│
         │                       │                       │
         │                    6. API Response            │
         │◄──────────────────────┼───────────────────────┤
```

### Flujo de Autenticación

1. **Cliente se autentica** con Keycloak
2. **Keycloak emite un JWT** con roles y claims
3. **Cliente envía JWT** en header Authorization
4. **Microservicio valida JWT** con Keycloak
5. **Autorización basada en roles** según endpoint
6. **Respuesta al cliente** si está autorizado

## ⚙️ Configuración de Keycloak

### 1. Crear Realm

```bash
# Realm Name: grade-management
# O usar el realm existente de tu aplicación
```

### 2. Crear Cliente

```json
{
  "clientId": "grade-management-service",
  "protocol": "openid-connect",
  "publicClient": false,
  "bearerOnly": true,
  "standardFlowEnabled": false,
  "serviceAccountsEnabled": false,
  "directAccessGrantsEnabled": false,
  "fullScopeAllowed": true
}
```

### 3. Configurar Roles

Crear los siguientes roles en el realm:

- **ADMIN**: Acceso completo al sistema
- **TEACHER**: Gestión de calificaciones
- **STUDENT**: Solo lectura de sus propias calificaciones

### 4. Configurar Mappers

Asegurar que los roles se incluyan en el JWT:

```json
{
  "name": "realm roles",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-usermodel-realm-role-mapper",
  "config": {
    "access.token.claim": "true",
    "claim.name": "realm_access.roles",
    "jsonType.label": "String",
    "multivalued": "true"
  }
}
```

## 🔧 Configuración del Microservicio

### Variables de Entorno Requeridas

```bash
# Keycloak Configuration
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak-server:8080/realms/grade-management

# Database
SPRING_DATA_MONGODB_URI=mongodb://username:password@host:port/database

# Server
SERVER_PORT=8081
```

### Configuración application.yml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/grade-management}

app:
  security:
    jwt:
      allowed-roles:
        - ADMIN
        - TEACHER
        - STUDENT
      public-endpoints:
        - /actuator/health
        - /api/v1/public/**
```

## 👤 Roles y Permisos

### ADMIN
- ✅ Ver todas las calificaciones
- ✅ Crear calificaciones
- ✅ Actualizar calificaciones
- ✅ Eliminar calificaciones (lógico)
- ✅ Restaurar calificaciones
- ✅ Ver calificaciones inactivas
- ✅ Acceso a todos los cursos
- ✅ Gestión de notificaciones

### TEACHER
- ✅ Ver todas las calificaciones
- ✅ Crear calificaciones
- ✅ Actualizar calificaciones
- ✅ Ver calificaciones por curso
- ✅ Gestión limitada de notificaciones
- ❌ Eliminar calificaciones
- ❌ Ver calificaciones inactivas

### STUDENT
- ✅ Ver sus propias calificaciones únicamente
- ✅ Ver notificaciones de sus calificaciones
- ❌ Crear/actualizar/eliminar calificaciones
- ❌ Ver calificaciones de otros estudiantes

## 🌐 Endpoints de Seguridad

### Endpoints Públicos (Sin autenticación)
```
GET /actuator/health
GET /api/v1/public/health
GET /api/v1/public/info
```

### Endpoints Protegidos

| Endpoint | ADMIN | TEACHER | STUDENT | Descripción |
|----------|-------|---------|---------|-------------|
| `GET /api/v1/grades` | ✅ | ✅ | ❌ | Ver todas las calificaciones |
| `GET /api/v1/grades/{id}` | ✅ | ✅ | ✅ | Ver calificación específica |
| `GET /api/v1/grades/student/{studentId}` | ✅ | ✅ | ✅* | Ver calificaciones del estudiante |
| `GET /api/v1/grades/course/{courseId}` | ✅ | ✅ | ❌ | Ver calificaciones del curso |
| `POST /api/v1/grades` | ✅ | ✅ | ❌ | Crear calificación |
| `PUT /api/v1/grades/{id}` | ✅ | ✅ | ❌ | Actualizar calificación |
| `DELETE /api/v1/grades/{id}` | ✅ | ❌ | ❌ | Eliminar calificación |
| `PUT /api/v1/grades/{id}/restore` | ✅ | ❌ | ❌ | Restaurar calificación |
| `GET /api/v1/grades/inactive` | ✅ | ❌ | ❌ | Ver calificaciones inactivas |

*\* Estudiantes solo pueden ver sus propias calificaciones*

## 🔨 Configuración de Desarrollo

### 1. Ejecutar Keycloak Local

```bash
# Con Docker
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:latest start-dev

# Acceder a: http://localhost:8080
# Admin Console: admin / admin
```

### 2. Configurar Realm de Desarrollo

1. Crear realm: `grade-management`
2. Crear cliente: `grade-management-service`
3. Crear usuarios de prueba:
   - `admin@test.com` (rol: ADMIN)
   - `teacher@test.com` (rol: TEACHER)
   - `student@test.com` (rol: STUDENT)

### 3. Ejecutar Microservicio

```bash
# Con perfil de desarrollo
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# O con JAR
java -jar -Dspring.profiles.active=dev target/vg-ms-grade-management-0.0.1-SNAPSHOT.jar
```

## 🚀 Configuración de Producción

### 1. Variables de Entorno

```bash
export SPRING_PROFILES_ACTIVE=prod
export KEYCLOAK_ISSUER_URI=https://your-keycloak-server.com/realms/your-realm
export SPRING_DATA_MONGODB_URI=mongodb://user:pass@prod-mongo:27017/grades_db
export SERVER_PORT=8081
```

### 2. Configuración SSL/TLS

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 3. Configuración de CORS

```yaml
cors:
  allowed-origins: 
    - https://your-frontend.com
    - https://your-admin-panel.com
```

## 🧪 Pruebas de Seguridad

### 1. Obtener Token JWT

```bash
# Obtener token de Keycloak
curl -X POST http://localhost:8080/realms/grade-management/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=grade-management-client" \
  -d "username=teacher@test.com" \
  -d "password=password123"
```

### 2. Probar Endpoints Protegidos

```bash
# Con token válido
curl -X GET http://localhost:8081/api/v1/grades \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Sin token (debería devolver 401)
curl -X GET http://localhost:8081/api/v1/grades

# Con rol insuficiente (debería devolver 403)
curl -X DELETE http://localhost:8081/api/v1/grades/123 \
  -H "Authorization: Bearer STUDENT_JWT_TOKEN"
```

### 3. Probar Endpoints Públicos

```bash
# No requiere autenticación
curl -X GET http://localhost:8081/api/v1/public/health
curl -X GET http://localhost:8081/actuator/health
```

## 🔍 Troubleshooting

### Error: "Invalid token issuer"

**Causa**: La URL del issuer en Keycloak no coincide con la configuración.

**Solución**:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://correct-keycloak-url:8080/realms/correct-realm-name
```

### Error: "No authorities found"

**Causa**: Los roles no se están mapeando correctamente desde el JWT.

**Solución**: Verificar el `KeycloakJwtAuthoritiesConverter` y los mappers en Keycloak.

### Error: "Access Denied"

**Causa**: El usuario no tiene los roles necesarios.

**Solución**: 
1. Verificar que el usuario tiene los roles asignados en Keycloak
2. Verificar que los roles están en el JWT token
3. Verificar las anotaciones `@PreAuthorize` en los endpoints

### Error: "JWT validation failed"

**Causa**: El token puede estar expirado o ser inválido.

**Solución**:
1. Verificar que Keycloak esté ejecutándose
2. Verificar la conectividad entre el microservicio y Keycloak
3. Verificar la configuración del `jwk-set-uri`

## 📚 Referencias

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Boot Security](https://spring.io/projects/spring-security)
- [JWT Specification](https://tools.ietf.org/html/rfc7519)

## 📞 Soporte

Para problemas de seguridad o configuración, contactar al equipo de desarrollo:

- Email: dev-team@vallegrande.edu.pe
- Slack: #security-support
- Wiki: [Security Guidelines](https://wiki.company.com/security)

---

*Última actualización: Enero 2025*
*Versión del microservicio: 1.0.0*
