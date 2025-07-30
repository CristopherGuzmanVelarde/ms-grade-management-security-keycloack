package pe.edu.vallegrande.vg_ms_grade_management.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades de configuración para la seguridad de la aplicación.
 */
@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * Configuración JWT
     */
    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
        /**
         * Roles permitidos en la aplicación
         */
        private List<String> allowedRoles = List.of("ADMIN", "TEACHER", "STUDENT");

        /**
         * Endpoints públicos que no requieren autenticación
         */
        private List<String> publicEndpoints = List.of(
                "/actuator/health",
                "/api/v1/grades/public/**"
        );
    }

    public List<String> getPublicEndpoints() {
        return jwt.getPublicEndpoints();
    }

    public List<String> getAllowedRoles() {
        return jwt.getAllowedRoles();
    }
}
