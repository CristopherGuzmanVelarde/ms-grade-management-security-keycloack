package pe.edu.vallegrande.vg_ms_grade_management.infrastructure.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convertidor que extrae los roles y autoridades de un JWT de Keycloak.
 * Keycloak almacena los roles en el claim "realm_access.roles" y "resource_access.{client-id}.roles".
 */
public class KeycloakJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRealmRoles(jwt);
        authorities.addAll(extractResourceRoles(jwt));
        return authorities;
    }

    /**
     * Extrae los roles del realm de Keycloak.
     *
     * @param jwt Token JWT
     * @return Collection de GrantedAuthority con los roles del realm
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return List.of();
        }

        List<String> roles = (List<String>) realmAccess.get(ROLES_CLAIM);
        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * Extrae los roles de recursos específicos de Keycloak.
     *
     * @param jwt Token JWT
     * @return Collection de GrantedAuthority con los roles de recursos
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) {
            return List.of();
        }

        return resourceAccess.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(resource -> resource.containsKey(ROLES_CLAIM))
                .flatMap(resource -> {
                    List<String> roles = (List<String>) resource.get(ROLES_CLAIM);
                    return roles.stream();
                })
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
