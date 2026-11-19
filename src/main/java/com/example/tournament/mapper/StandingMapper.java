package com.example.tournament.mapper;

import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.entity.Standing;
import org.springframework.stereotype.Component;

@Component
public class StandingMapper {

    public StandingResponse toResponse(Standing entity) {
        if (entity == null) return null;
        StandingResponse response = new StandingResponse();
        response.setId(entity.getId());

        if (entity.getTournament() != null) {
            response.setTournamentId(entity.getTournament().getId());
        }
        if (entity.getTeam() != null) {
            response.setTeamId(entity.getTeam().getId());
            response.setTeamName(entity.getTeam().getName());
        }

        response.setPosition(entity.getPosition());
        response.setMatchesPlayed(entity.getMatchesPlayed());
        response.setWins(entity.getWins());
        response.setDraws(entity.getDraws());
        response.setLosses(entity.getLosses());
        response.setGoalsFor(entity.getGoalsFor());
        response.setGoalsAgainst(entity.getGoalsAgainst());
        response.setGoalDifference(entity.getGoalDifference());
        response.setPoints(entity.getPoints());
        response.setGroup(entity.getGroup());

        return response;
    }
}
