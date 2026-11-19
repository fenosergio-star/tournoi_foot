package com.example.tournament.service;

import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.request.UpdateMatchResultRequest;
import com.example.tournament.dto.response.BracketResponse;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.TournamentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface MatchService {

    MatchResponse getMatchById(Long id);

    TournamentType getTournamentType(Long tournamentId);

    List<MatchResponse> getMatchesByTournament(Long tournamentId);

    Page<MatchResponse> getMatchesByTournament(Long tournamentId, Pageable pageable);

    Page<MatchResponse> searchMatchesByTournament(Long tournamentId, MatchStatus status, MatchPhase phase,
                                                  LocalDate date, Pageable pageable);

    List<MatchResponse> getMatchesByTournamentAndPhase(Long tournamentId, String phase);

    MatchResponse updateMatchResult(Long matchId, UpdateMatchResultRequest request);

    MatchResponse rescheduleMatch(Long matchId, RescheduleMatchRequest request);

    void cancelMatch(Long matchId);

    List<MatchResponse> getMatchesByTeam(Long tournamentId, Long teamId);

    List<MatchResponse> getUpcomingMatches(Long tournamentId);

    BracketResponse getBracket(Long tournamentId);
}
