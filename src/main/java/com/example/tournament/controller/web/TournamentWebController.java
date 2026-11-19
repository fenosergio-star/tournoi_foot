package com.example.tournament.controller.web;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.Country;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.TournamentType;
import com.example.tournament.service.MatchGenerationService;
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
@RequestMapping("/tournaments")
public class TournamentWebController {

    private final TournamentService tournamentService;
    private final TeamService teamService;
    private final MatchGenerationService matchGenerationService;

    public TournamentWebController(TournamentService tournamentService,
                                   TeamService teamService,
                                   MatchGenerationService matchGenerationService) {
        this.tournamentService = tournamentService;
        this.teamService = teamService;
        this.matchGenerationService = matchGenerationService;
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
                       @RequestParam(defaultValue = "9") int size,
                       @RequestParam(required = false) TournamentStatus status,
                       @RequestParam(required = false) String sport,
                       @RequestParam(required = false) String name,
                       @RequestParam(defaultValue = "startDate") String sort,
                       @RequestParam(defaultValue = "desc") String direction,
                       Model model) {
        String safeSort = switch (sort) {
            case "name", "sport", "type", "status", "startDate", "endDate" -> sort;
            default -> "startDate";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<TournamentResponse> result = tournamentService.searchTournaments(status, sport, name,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(dir, safeSort)));
        model.addAttribute("tournaments", result.getContent());
        model.addAttribute("page", result);
        model.addAttribute("statuses", TournamentStatus.values());
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterSport", sport);
        model.addAttribute("filterName", name);
        model.addAttribute("sort", safeSort);
        model.addAttribute("direction", dir == Sort.Direction.ASC ? "asc" : "desc");
        model.addAttribute("currentPage", "tournaments");
        return "tournaments";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("tournament", new CreateTournamentRequest());
        model.addAttribute("types", TournamentType.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("currentPage", "tournaments");
        return "tournament-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("tournament") CreateTournamentRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("types", TournamentType.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("currentPage", "tournaments");
            return "tournament-form";
        }
        tournamentService.createTournament(request);
        redirectAttributes.addFlashAttribute("successMessage", "Tournoi créé avec succès");
        return "redirect:/tournaments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TournamentResponse tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);
        model.addAttribute("types", TournamentType.values());
        model.addAttribute("statuses", TournamentStatus.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("currentPage", "tournaments");
        return "tournament-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute UpdateTournamentRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            tournamentService.updateTournament(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Tournoi mis à jour avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tournamentService.deleteTournament(id);
            redirectAttributes.addFlashAttribute("successMessage", "Tournoi supprimé avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam TournamentStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            tournamentService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Statut mis à jour : " + status);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/generate-matches")
    public String generateMatches(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            int numberOfMatches;
            var type = tournamentService.getTournamentById(id).getType();
            if (type == TournamentType.ELIMINATION_DIRECTE) {
                numberOfMatches = matchGenerationService.generateKnockoutMatches(id).size();
            } else if (type == TournamentType.POULES_ELIMINATION) {
                numberOfMatches = matchGenerationService.generateGroupStageMatches(id).size();
            } else {
                numberOfMatches = matchGenerationService.generateChampionshipMatches(id).size();
            }
            redirectAttributes.addFlashAttribute("successMessage",
                "Matchs générés avec succès (" + numberOfMatches + " matchs)");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/generate-elimination")
    public String generateElimination(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            int numberOfMatches = matchGenerationService.generateEliminationPhase(id).size();
            redirectAttributes.addFlashAttribute("successMessage",
                "Phase finale générée avec succès (" + numberOfMatches + " matchs)");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/qualify")
    public String qualifyTopTeams(@PathVariable Long id,
                                  @RequestParam(defaultValue = "3") int count,
                                  RedirectAttributes redirectAttributes) {
        try {
            List<String> qualified = tournamentService.qualifyTopTeams(id, null, count);
            if (qualified.isEmpty()) {
                redirectAttributes.addFlashAttribute("infoMessage",
                    "Aucune nouvelle équipe qualifiée (déjà inscrites)");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                    qualified.size() + " équipe(s) qualifiée(s) en Ligue des Champions : "
                        + String.join(", ", qualified));
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/teams")
    public String registerTeam(@PathVariable Long id, @RequestParam Long teamId,
                               RedirectAttributes redirectAttributes) {
        try {
            tournamentService.registerTeam(id, teamId);
            redirectAttributes.addFlashAttribute("successMessage", "Équipe inscrite avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/teams/{teamId}/remove")
    public String removeTeam(@PathVariable Long id, @PathVariable Long teamId,
                             RedirectAttributes redirectAttributes) {
        try {
            tournamentService.removeTeam(id, teamId);
            redirectAttributes.addFlashAttribute("successMessage", "Équipe retirée avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tournaments/" + id;
    }
}
