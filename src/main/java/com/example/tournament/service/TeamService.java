package com.example.tournament.service;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.CreatePlayerRequest;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.dto.response.PlayerResponse;
import com.example.tournament.entity.Country;
import com.example.tournament.entity.TeamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest request);

    TeamResponse getTeamById(Long id);

    Page<TeamResponse> getAllTeams(Pageable pageable);

    Page<TeamResponse> searchTeams(String name, Country country, TeamStatus status, Pageable pageable);

    List<TeamResponse> getAllTeams();

    TeamResponse updateTeam(Long id, UpdateTeamRequest request);

    void deleteTeam(Long id);

    PlayerResponse addPlayer(Long teamId, CreatePlayerRequest request);

    List<PlayerResponse> getPlayersByTeam(Long teamId);

    void removePlayer(Long teamId, Long playerId);

    List<TeamResponse> getAvailableTeams(Long tournamentId);

    List<TeamResponse> getTeamsOfTournament(Long tournamentId);
}
