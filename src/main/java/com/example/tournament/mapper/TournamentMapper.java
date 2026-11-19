package com.example.tournament.mapper;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentStatus;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

    public Tournament toEntity(CreateTournamentRequest request) {
        if (request == null) return null;
        Tournament tournament = new Tournament();
        tournament.setName(request.getName());
        tournament.setSport(request.getSport());
        tournament.setDescription(request.getDescription());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setLocation(request.getLocation());
        tournament.setCountry(request.getCountry());
        tournament.setMaxTeams(request.getMaxTeams());
        tournament.setType(request.getType());
        if (request.getPointsWin() != null) tournament.setPointsWin(request.getPointsWin());
        if (request.getPointsDraw() != null) tournament.setPointsDraw(request.getPointsDraw());
        if (request.getPointsLoss() != null) tournament.setPointsLoss(request.getPointsLoss());
        if (request.getTiebreakers() != null && !request.getTiebreakers().isBlank()) {
            tournament.setTiebreakers(request.getTiebreakers());
        }
        tournament.setStatus(TournamentStatus.BROUILLON);
        return tournament;
    }

    public TournamentResponse toResponse(Tournament entity) {
        if (entity == null) return null;
        TournamentResponse response = new TournamentResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setSport(entity.getSport());
        response.setDescription(entity.getDescription());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setLocation(entity.getLocation());
        response.setCountry(entity.getCountry());
        response.setMaxTeams(entity.getMaxTeams());
        response.setRegisteredTeams(entity.getTeams() != null ? entity.getTeams().size() : 0);
        response.setType(entity.getType());
        response.setStatus(entity.getStatus());
        response.setPointsWin(entity.getPointsWin());
        response.setPointsDraw(entity.getPointsDraw());
        response.setPointsLoss(entity.getPointsLoss());
        response.setTiebreakers(entity.getTiebreakers());
        return response;
    }
}
