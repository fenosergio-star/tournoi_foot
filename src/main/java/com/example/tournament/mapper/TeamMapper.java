package com.example.tournament.mapper;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.dto.response.PlayerResponse;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.Player;
import com.example.tournament.entity.TeamStatus;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TeamMapper {

    public Team toEntity(CreateTeamRequest request) {
        if (request == null) return null;
        Team team = new Team();
        team.setName(request.getName());
        team.setLogo(request.getLogo());
        team.setCity(request.getCity());
        team.setCoach(request.getCoach());
        team.setCountry(request.getCountry());
        team.setStatus(TeamStatus.ACTIVE);
        return team;
    }

    public TeamResponse toResponse(Team entity) {
        if (entity == null) return null;
        TeamResponse response = new TeamResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setLogo(entity.getLogo());
        response.setCity(entity.getCity());
        response.setCoach(entity.getCoach());
        response.setCountry(entity.getCountry());
        response.setStatus(entity.getStatus());
        response.setPlayerCount(entity.getPlayers() != null ? entity.getPlayers().size() : 0);
        response.setTournamentIds(
            entity.getTournaments() != null
                ? entity.getTournaments().stream().map(t -> t.getId()).collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
        return response;
    }

    public PlayerResponse toPlayerResponse(Player player) {
        if (player == null) return null;
        PlayerResponse response = new PlayerResponse();
        response.setId(player.getId());
        response.setFirstName(player.getFirstName());
        response.setLastName(player.getLastName());
        response.setPosition(player.getPosition());
        response.setNumber(player.getNumber());
        if (player.getTeam() != null) {
            response.setTeamId(player.getTeam().getId());
            response.setTeamName(player.getTeam().getName());
        }
        return response;
    }
}
