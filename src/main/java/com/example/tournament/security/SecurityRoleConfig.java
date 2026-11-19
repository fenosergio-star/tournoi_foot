package com.example.tournament.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Définit la hiérarchie des rôles de l'application :
 * ADMIN &gt; MANAGER &gt; OPERATOR &gt; USER.
 * <p>
 * Un ADMIN hérite donc automatiquement des droits MANAGER, OPERATOR et USER,
 * côté URL (AuthorizationManagerFactory) comme côté méthode (@PreAuthorize).
 */
@Configuration
@EnableMethodSecurity
public class SecurityRoleConfig {

    public static final String ROLE_HIERARCHY =
            "ROLE_ADMIN > ROLE_MANAGER\n"
            + "ROLE_MANAGER > ROLE_OPERATOR\n"
            + "ROLE_OPERATOR > ROLE_USER";

    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(ROLE_HIERARCHY);
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public AuthorizationManagerFactory<RequestAuthorizationContext> authorizationManagerFactory(
            RoleHierarchy roleHierarchy) {
        DefaultAuthorizationManagerFactory<RequestAuthorizationContext> factory =
                new DefaultAuthorizationManagerFactory<>();
        factory.setRoleHierarchy(roleHierarchy);
        return factory;
    }
}
