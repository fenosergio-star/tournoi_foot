package com.example.tournament.mapper;

import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.util.PhaseLabels;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    public MatchResponse toResponse(Match entity) {
        if (entity == null) return null;
        MatchResponse response = new MatchResponse();
        response.setId(entity.getId());

        if (entity.getTournament() != null) {
            response.setTournamentId(entity.getTournament().getId());
            response.setTournamentName(entity.getTournament().getName());
        }
        if (entity.getHomeTeam() != null) {
            response.setHomeTeamId(entity.getHomeTeam().getId());
            response.setHomeTeamName(entity.getHomeTeam().getName());
        }
        if (entity.getAwayTeam() != null) {
            response.setAwayTeamId(entity.getAwayTeam().getId());
            response.setAwayTeamName(entity.getAwayTeam().getName());
        }

        response.setMatchDate(entity.getMatchDate());
        response.setMatchTime(entity.getMatchTime());
        response.setField(entity.getField());
        response.setPhase(entity.getPhase());
        response.setStatus(entity.getStatus());
        response.setHomeScore(entity.getHomeScore());
        response.setAwayScore(entity.getAwayScore());
        response.setHomePenalties(entity.getHomePenalties());
        response.setAwayPenalties(entity.getAwayPenalties());
        response.setRound(entity.getRound());
        response.setBracketPosition(entity.getBracketPosition());
        response.setPhaseLabel(PhaseLabels.label(entity.getPhase()));
        response.setTeamsReady(entity.getHomeTeam() != null && entity.getAwayTeam() != null);
        response.setGroup(entity.getGroup());
        response.setLeg(entity.getLeg());

        if (entity.getStatus() == MatchStatus.COMPLETED && entity.getWinner() != null) {
            response.setWinnerName(entity.getWinner().getName());
        }

        return response;
    }
}
