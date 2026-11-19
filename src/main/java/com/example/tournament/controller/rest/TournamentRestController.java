package com.example.tournament.controller.rest;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.service.RankingService;
import com.example.tournament.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentRestController {

    private final TournamentService tournamentService;
    private final RankingService rankingService;

    public TournamentRestController(TournamentService tournamentService,
                                    RankingService rankingService) {
        this.tournamentService = tournamentService;
        this.rankingService = rankingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TournamentResponse> createTournament(@Valid @RequestBody CreateTournamentRequest request) {
        TournamentResponse response = tournamentService.createTournament(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<TournamentResponse>> getAllTournaments(
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable,
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(tournamentService.searchTournaments(status, sport, name, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TournamentResponse>> getAllTournamentsList() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        TournamentResponse response = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TournamentResponse> updateTournament(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateTournamentRequest request) {
        TournamentResponse response = tournamentService.updateTournament(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TournamentResponse> updateStatus(@PathVariable Long id,
                                                           @RequestBody TournamentStatus status) {
        TournamentResponse response = tournamentService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/teams")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<TournamentResponse> registerTeam(@PathVariable Long id,
                                                           @RequestParam Long teamId) {
        TournamentResponse response = tournamentService.registerTeam(id, teamId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TournamentResponse> removeTeam(@PathVariable Long id,
                                                         @PathVariable Long teamId) {
        TournamentResponse response = tournamentService.removeTeam(id, teamId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/qualify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<List<String>> qualifyTopTeams(@PathVariable Long id,
                                                        @RequestParam(required = false) Long targetId,
                                                        @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(tournamentService.qualifyTopTeams(id, targetId, count));
    }

    @GetMapping("/{id}/ranking")
    public ResponseEntity<List<StandingResponse>> getRanking(@PathVariable Long id) {
        return ResponseEntity.ok(rankingService.getRankingByTournament(id));
    }
}
