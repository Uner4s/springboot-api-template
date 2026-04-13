package com.example.backend.security.permissions;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("permissionEvaluator")
public class AppPermissionEvaluator {

    public boolean hasPermission(Authentication authentication, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth ->
                        auth.equals("PERMISSION_" + permissionName) ||
                        auth.equals("PERMISSION_SUPER_ADMIN")
                );
    }
}
