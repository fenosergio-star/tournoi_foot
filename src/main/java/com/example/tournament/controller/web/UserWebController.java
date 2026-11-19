package com.example.tournament.controller.web;

import com.example.tournament.dto.request.CreateUserRequest;
import com.example.tournament.entity.Role;
import com.example.tournament.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class UserWebController {

    private final UserService userService;

    public UserWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        populateModel(model);
        return "admin/users";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("newUser") CreateUserRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateModel(model);
            model.addAttribute("openCreateForm", true);
            return "admin/users";
        }
        try {
            userService.createUser(request);
            redirectAttributes.addFlashAttribute("successMessage", "Utilisateur créé avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam Role role,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateRole(id, role, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Rôle mis à jour avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Mot de passe réinitialisé avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Utilisateur supprimé avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void populateModel(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("roles", Role.values());
        if (!model.containsAttribute("newUser")) {
            model.addAttribute("newUser", new CreateUserRequest());
        }
        model.addAttribute("currentPage", "users");
    }
}
