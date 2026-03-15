
package com.financeportal.backend.Config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // realm_access claim'ini al
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.isEmpty()) {
            return new ArrayList<>();
        }

        // roles listesini al
        Object rolesObj = realmAccess.get("roles");

        if (!(rolesObj instanceof List)) {
            return new ArrayList<>();
        }

        List<String> roles = (List<String>) rolesObj;
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Her role için ROLE_ prefix ekle
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        return authorities;
    }
}
