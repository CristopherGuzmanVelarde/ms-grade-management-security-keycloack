package pe.edu.vallegrande.vg_ms_grade_management.infrastructure.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador público para endpoints que no requieren autenticación.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicRest {

    /**
     * Endpoint público para verificar el estado del servicio.
     *
     * @return Estado del servicio
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "service", "vg-ms-grade-management",
                "timestamp", LocalDateTime.now(),
                "message", "Grade Management Service is running"
        ));
    }

    /**
     * Endpoint público para obtener información del servicio.
     *
     * @return Información del servicio
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> info() {
        return Mono.just(Map.of(
                "name", "Grade Management Microservice",
                "version", "1.0.0",
                "description", "Microservicio para gestión de calificaciones",
                "security", "OAuth2 + JWT (Keycloak)",
                "endpoints", Map.of(
                        "authenticated", "/api/v1/grades/**",
                        "public", "/api/v1/public/**"
                )
        ));
    }
}
