package com.example.tournament.controller.rest;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.CreatePlayerRequest;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.PlayerResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.entity.Country;
import com.example.tournament.entity.TeamStatus;
import com.example.tournament.service.TeamService;
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
@RequestMapping("/api/teams")
public class TeamRestController {

    private final TeamService teamService;

    public TeamRestController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse response = teamService.createTeam(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<TeamResponse>> getAllTeams(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Country country,
            @RequestParam(required = false) TeamStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(teamService.searchTeams(name, country, status, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TeamResponse>> getAllTeamsList() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateTeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/players")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<PlayerResponse> addPlayer(@PathVariable Long teamId,
                                                    @Valid @RequestBody CreatePlayerRequest request) {
        PlayerResponse response = teamService.addPlayer(teamId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{teamId}/players")
    public ResponseEntity<List<PlayerResponse>> getPlayersByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getPlayersByTeam(teamId));
    }

    @DeleteMapping("/{teamId}/players/{playerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> removePlayer(@PathVariable Long teamId, @PathVariable Long playerId) {
        teamService.removePlayer(teamId, playerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available/{tournamentId}")
    public ResponseEntity<List<TeamResponse>> getAvailableTeams(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(teamService.getAvailableTeams(tournamentId));
    }
}
