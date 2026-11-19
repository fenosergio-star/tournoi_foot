package com.example.tournament.controller.web;

import com.example.tournament.dto.request.CreatePlayerRequest;
import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.PlayerResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.entity.Country;
import com.example.tournament.entity.TeamStatus;
import com.example.tournament.service.TeamService;
import com.example.tournament.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.util.List;

@Controller
@RequestMapping("/teams")
public class TeamWebController {

    private final TeamService teamService;
    private final TournamentService tournamentService;

    public TeamWebController(TeamService teamService, TournamentService tournamentService) {
        this.teamService = teamService;
        this.tournamentService = tournamentService;
    }

    @InitBinder
    public void bindCountry(WebDataBinder binder) {
        binder.registerCustomEditor(Country.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : Country.valueOf(text.trim()));
            }
        });
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "12") int size,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Country country,
                       @RequestParam(required = false) TeamStatus status,
                       @RequestParam(defaultValue = "name") String sort,
                       @RequestParam(defaultValue = "asc") String direction,
                       Model model) {
        String safeSort = switch (sort) {
            case "name", "city", "country", "status" -> sort;
            default -> "name";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<TeamResponse> result = teamService.searchTeams(name, country, status,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(dir, safeSort)));
        model.addAttribute("teams", result.getContent());
        model.addAttribute("page", result);
        model.addAttribute("countries", Country.values());
        model.addAttribute("statuses", TeamStatus.values());
        model.addAttribute("filterName", name);
        model.addAttribute("filterCountry", country);
        model.addAttribute("filterStatus", status);
        model.addAttribute("sort", safeSort);
        model.addAttribute("direction", dir == Sort.Direction.ASC ? "asc" : "desc");
        model.addAttribute("currentPage", "teams");
        return "teams";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("team", new CreateTeamRequest());
        model.addAttribute("isEdit", false);
        model.addAttribute("currentPage", "teams");
        return "team-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("team") CreateTeamRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("currentPage", "teams");
            return "team-form";
        }
        teamService.createTeam(request);
        redirectAttributes.addFlashAttribute("successMessage", "Équipe créée avec succès");
        return "redirect:/teams";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        TeamResponse team = teamService.getTeamById(id);
        List<PlayerResponse> players = teamService.getPlayersByTeam(id);
        model.addAttribute("team", team);
        model.addAttribute("players", players);
        model.addAttribute("tournaments", tournamentService.getAllTournaments());
        model.addAttribute("currentPage", "teams");
        return "team-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TeamResponse team = teamService.getTeamById(id);
        model.addAttribute("team", team);
        model.addAttribute("isEdit", true);
        model.addAttribute("currentPage", "teams");
        return "team-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute UpdateTeamRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            teamService.updateTeam(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Équipe mise à jour avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            teamService.deleteTeam(id);
            redirectAttributes.addFlashAttribute("successMessage", "Équipe supprimée avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/teams";
    }

    @PostMapping("/{id}/players")
    public String addPlayer(@PathVariable Long id,
                            @ModelAttribute CreatePlayerRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            teamService.addPlayer(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Joueur ajouté avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{teamId}/players/{playerId}/remove")
    public String removePlayer(@PathVariable Long teamId, @PathVariable Long playerId,
                               RedirectAttributes redirectAttributes) {
        try {
            teamService.removePlayer(teamId, playerId);
            redirectAttributes.addFlashAttribute("successMessage", "Joueur retiré avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/teams/" + teamId;
    }
}
