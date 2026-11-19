package com.example.tournament.controller.web;

import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.request.UpdateMatchResultRequest;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.service.MatchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/matches")
public class MatchWebController {

    private final MatchService matchService;

    public MatchWebController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public String list() {
        return "redirect:/tournaments";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        MatchResponse match = matchService.getMatchById(id);
        UpdateMatchResultRequest resultRequest = new UpdateMatchResultRequest();
        if (match.getStatus() == com.example.tournament.entity.MatchStatus.COMPLETED) {
            resultRequest.setHomeScore(match.getHomeScore());
            resultRequest.setAwayScore(match.getAwayScore());
            resultRequest.setHomePenalties(match.getHomePenalties());
            resultRequest.setAwayPenalties(match.getAwayPenalties());
        }
        RescheduleMatchRequest scheduleRequest = new RescheduleMatchRequest();
        scheduleRequest.setMatchDate(match.getMatchDate());
        scheduleRequest.setMatchTime(match.getMatchTime());
        scheduleRequest.setField(match.getField());
        model.addAttribute("match", match);
        model.addAttribute("resultRequest", resultRequest);
        model.addAttribute("scheduleRequest", scheduleRequest);
        model.addAttribute("currentPage", "tournaments");
        return "match-detail";
    }

    @PostMapping("/{id}/schedule")
    public String reschedule(@PathVariable Long id,
                             @ModelAttribute("scheduleRequest") RescheduleMatchRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            matchService.rescheduleMatch(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Match reporté avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/matches/" + id;
    }

    @PostMapping("/{id}/result")
    public String updateResult(@PathVariable Long id,
                               @ModelAttribute("resultRequest") UpdateMatchResultRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            matchService.updateMatchResult(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Résultat enregistré avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        MatchResponse match = matchService.getMatchById(id);
        return "redirect:/tournaments/" + match.getTournamentId();
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            matchService.cancelMatch(id);
            redirectAttributes.addFlashAttribute("successMessage", "Match annulé avec succès");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        MatchResponse match = matchService.getMatchById(id);
        return "redirect:/tournaments/" + match.getTournamentId();
    }
}
