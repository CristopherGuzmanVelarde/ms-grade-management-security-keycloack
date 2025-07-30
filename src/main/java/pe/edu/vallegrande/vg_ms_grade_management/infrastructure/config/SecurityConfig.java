package pe.edu.vallegrande.vg_ms_grade_management.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad para el microservicio de gestión de calificaciones.
 * Implementa OAuth2 Resource Server con JWT usando Keycloak como proveedor de identidad.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    /**
     * Configura la cadena de filtros de seguridad para WebFlux.
     *
     * @param http Configuración de seguridad HTTP
     * @return SecurityWebFilterChain configurado
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Deshabilitar CSRF para APIs REST
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                
                // Configurar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Configurar autorización de endpoints
                .authorizeExchange(exchanges -> exchanges
                        // Endpoints públicos
                        .pathMatchers(securityProperties.getPublicEndpoints().toArray(String[]::new))
                        .permitAll()
                        
                        // Endpoints específicos por rol
                        .pathMatchers("/api/v1/grades/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/courses/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/notifications/admin/**").hasRole("ADMIN")
                        
                        // Endpoints para profesores
                        .pathMatchers("/api/v1/grades/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        .pathMatchers("/api/v1/courses/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        
                        // Todos los demás endpoints requieren autenticación
                        .anyExchange().authenticated()
                )
                
                // Configurar OAuth2 Resource Server con JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(reactiveJwtAuthenticationConverter())
                        )
                )
                
                // Configurar manejo de excepciones
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                
                .build();
    }

    /**
     * Configura CORS para permitir peticiones desde el frontend.
     *
     * @return CorsConfigurationSource configurado
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Convierte el JWT a una autenticación de Spring Security para WebFlux.
     *
     * @return ReactiveJwtAuthenticationConverter configurado
     */
    @Bean
    public ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> 
            Flux.fromIterable(new KeycloakJwtAuthoritiesConverter().convert(jwt))
        );
        return converter;
    }
}
