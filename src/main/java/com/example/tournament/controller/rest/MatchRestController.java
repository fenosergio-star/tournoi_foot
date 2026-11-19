package com.example.tournament.controller.rest;

import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.request.UpdateMatchResultRequest;
import com.example.tournament.dto.response.BracketResponse;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.service.MatchGenerationService;
import com.example.tournament.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MatchRestController {

    private final MatchService matchService;
    private final MatchGenerationService matchGenerationService;

    public MatchRestController(MatchService matchService,
                               MatchGenerationService matchGenerationService) {
        this.matchService = matchService;
        this.matchGenerationService = matchGenerationService;
    }

    @PostMapping("/tournaments/{id}/generate-matches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<List<MatchResponse>> generateMatches(@PathVariable Long id) {
        var generated = switch (matchService.getTournamentType(id)) {
            case CHAMPIONNAT_SIMPLE, CHAMPIONNAT_ALLER_RETOUR ->
                matchGenerationService.generateChampionshipMatches(id);
            case ELIMINATION_DIRECTE ->
                matchGenerationService.generateKnockoutMatches(id);
            case POULES_ELIMINATION ->
                matchGenerationService.generateGroupStageMatches(id);
        };
        return new ResponseEntity<>(generated, HttpStatus.CREATED);
    }

    @PostMapping("/tournaments/{id}/generate-elimination")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<List<MatchResponse>> generateEliminationPhase(@PathVariable Long id) {
        return new ResponseEntity<>(matchGenerationService.generateEliminationPhase(id), HttpStatus.CREATED);
    }

    @GetMapping("/tournaments/{id}/matches")
    public ResponseEntity<List<MatchResponse>> getMatchesByTournament(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchesByTournament(id));
    }

    @GetMapping("/tournaments/{id}/matches/paged")
    public ResponseEntity<Page<MatchResponse>> getMatchesPaged(
            @PathVariable Long id,
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) MatchPhase phase,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "matchDate") Pageable pageable) {
        return ResponseEntity.ok(matchService.searchMatchesByTournament(id, status, phase, date, pageable));
    }

    @GetMapping("/tournaments/{id}/bracket")
    public ResponseEntity<BracketResponse> getBracket(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getBracket(id));
    }

    @GetMapping("/tournaments/{id}/matches/upcoming")
    public ResponseEntity<List<MatchResponse>> getUpcomingMatches(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getUpcomingMatches(id));
    }

    @GetMapping("/matches/{id}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    @PutMapping("/matches/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<MatchResponse> updateMatchResult(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateMatchResultRequest request) {
        MatchResponse response = matchService.updateMatchResult(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/matches/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<MatchResponse> rescheduleMatch(@PathVariable Long id,
                                                         @Valid @RequestBody RescheduleMatchRequest request) {
        return ResponseEntity.ok(matchService.rescheduleMatch(id, request));
    }

    @PostMapping("/matches/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> cancelMatch(@PathVariable Long id) {
        matchService.cancelMatch(id);
        return ResponseEntity.noContent().build();
    }
}
