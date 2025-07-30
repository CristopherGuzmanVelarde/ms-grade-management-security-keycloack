package pe.edu.vallegrande.vg_ms_grade_management.infrastructure.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Manejador personalizado para errores de autorización.
 * Se ejecuta cuando un usuario autenticado intenta acceder a un recurso para el cual no tiene permisos.
 */
@Slf4j
public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        log.warn("Access denied: {}", denied.getMessage());

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.FORBIDDEN.value(),
                "error", "Forbidden",
                "message", "You don't have permission to access this resource",
                "path", exchange.getRequest().getPath().value()
        );

        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(json.getBytes());
            return exchange.getResponse().writeWith(Mono.just(dataBuffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating access denied error response", e);
            return exchange.getResponse().setComplete();
        }
    }
}
