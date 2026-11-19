package com.example.tournament.service;

import com.example.tournament.dto.response.StandingResponse;

import java.util.List;

public interface RankingService {

    List<StandingResponse> getRankingByTournament(Long tournamentId);

    void recalculateRanking(Long tournamentId);
}
