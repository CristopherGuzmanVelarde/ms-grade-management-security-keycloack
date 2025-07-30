package pe.edu.vallegrande.vg_ms_grade_management.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilidad para acceder al contexto de seguridad en aplicaciones reactivas.
 * Proporciona métodos para obtener información del usuario autenticado.
 */
@Slf4j
@Component
public class SecurityContextUtils {

    /**
     * Obtiene el JWT del usuario autenticado.
     *
     * @return Mono con el JWT
     */
    public Mono<Jwt> getCurrentUserJwt() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .doOnNext(jwt -> log.debug("Current user JWT: {}", jwt.getSubject()));
    }

    /**
     * Obtiene el ID del usuario autenticado desde el JWT.
     *
     * @return Mono con el ID del usuario
     */
    public Mono<String> getCurrentUserId() {
        return getCurrentUserJwt()
                .map(jwt -> jwt.getClaimAsString("sub"));
    }

    /**
     * Obtiene el nombre de usuario del usuario autenticado.
     *
     * @return Mono con el nombre de usuario
     */
    public Mono<String> getCurrentUsername() {
        return getCurrentUserJwt()
                .map(jwt -> jwt.getClaimAsString("preferred_username"));
    }

    /**
     * Obtiene el email del usuario autenticado.
     *
     * @return Mono con el email
     */
    public Mono<String> getCurrentUserEmail() {
        return getCurrentUserJwt()
                .map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Obtiene los roles del usuario autenticado.
     *
     * @return Mono con la lista de roles
     */
    public Mono<List<String>> getCurrentUserRoles() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring(5)) // Remove "ROLE_" prefix
                        .collect(Collectors.toList()));
    }

    /**
     * Verifica si el usuario actual tiene un rol específico.
     *
     * @param role El rol a verificar
     * @return Mono con true si el usuario tiene el rol, false en caso contrario
     */
    public Mono<Boolean> hasRole(String role) {
        return getCurrentUserRoles()
                .map(roles -> roles.contains(role.toUpperCase()));
    }

    /**
     * Verifica si el usuario actual tiene alguno de los roles especificados.
     *
     * @param roles Los roles a verificar
     * @return Mono con true si el usuario tiene alguno de los roles, false en caso contrario
     */
    public Mono<Boolean> hasAnyRole(String... roles) {
        return getCurrentUserRoles()
                .map(userRoles -> {
                    for (String role : roles) {
                        if (userRoles.contains(role.toUpperCase())) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * Obtiene toda la información del usuario autenticado.
     *
     * @return Mono con UserInfo
     */
    public Mono<UserInfo> getCurrentUserInfo() {
        return getCurrentUserJwt()
                .flatMap(jwt -> {
                    UserInfo userInfo = UserInfo.builder()
                            .id(jwt.getClaimAsString("sub"))
                            .username(jwt.getClaimAsString("preferred_username"))
                            .email(jwt.getClaimAsString("email"))
                            .firstName(jwt.getClaimAsString("given_name"))
                            .lastName(jwt.getClaimAsString("family_name"))
                            .build();

                    return getCurrentUserRoles()
                            .map(roles -> {
                                userInfo.setRoles(roles);
                                return userInfo;
                            });
                });
    }

    /**
     * Información del usuario autenticado.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> roles;
    }
}
