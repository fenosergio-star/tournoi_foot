package com.example.tournament.config;

import com.example.tournament.entity.Country;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Expose les droits de l'utilisateur courant à toutes les vues Thymeleaf.
 * <p>
 * Cela permet d'afficher uniquement les actions autorisées (comme sur SofaScore)
 * au lieu de montrer des boutons qui échoueraient côté serveur. La hiérarchie
 * des rôles est reprise ici : ADMIN &gt; MANAGER &gt; OPERATOR &gt; USER.
 */
@ControllerAdvice(basePackages = "com.example.tournament.controller.web")
public class GlobalModelAttributes {

    @ModelAttribute
    public void addRoleAttributes(Model model, Authentication authentication) {
        Set<String> roles = extractRoles(authentication);

        boolean isAdmin = roles.contains("ADMIN");
        boolean isManager = roles.contains("MANAGER");
        boolean isOperator = roles.contains("OPERATOR");
        boolean isUser = roles.contains("USER");

        boolean authenticated = isAuthenticated(authentication);

        // Héritage des droits : un ADMIN possède les droits MANAGER, etc.
        boolean canManage = isAdmin || isManager;
        boolean canOperate = canManage || isOperator;

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isManager", isManager);
        model.addAttribute("isOperator", isOperator);
        model.addAttribute("isUser", isUser);
        model.addAttribute("canManage", canManage);
        model.addAttribute("canOperate", canOperate);
        model.addAttribute("currentUsername", authenticated ? authentication.getName() : null);
        model.addAttribute("currentRole", roleLabel(isAdmin, isManager, isOperator, isUser));
        model.addAttribute("countries", Country.values());
    }

    private Set<String> extractRoles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .collect(Collectors.toSet());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String roleLabel(boolean isAdmin, boolean isManager, boolean isOperator, boolean isUser) {
        if (isAdmin) return "Administrateur";
        if (isManager) return "Gestionnaire";
        if (isOperator) return "Opérateur";
        if (isUser) return "Consultation";
        return "Invité";
    }
}
