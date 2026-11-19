package com.example.tournament.service;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TournamentService {

    TournamentResponse createTournament(CreateTournamentRequest request);

    TournamentResponse getTournamentById(Long id);

    Page<TournamentResponse> getAllTournaments(Pageable pageable);

    Page<TournamentResponse> searchTournaments(TournamentStatus status, String sport, String name, Pageable pageable);

    List<TournamentResponse> getAllTournaments();

    TournamentResponse updateTournament(Long id, UpdateTournamentRequest request);

    void deleteTournament(Long id);

    TournamentResponse updateStatus(Long id, TournamentStatus status);

    TournamentResponse registerTeam(Long tournamentId, Long teamId);

    /**
     * Inscrit automatiquement une équipe dans tous les tournois ouverts de son pays
     * (statut BROUILLON ou INSCRIPTION). Appelé à la création d'une équipe.
     */
    void autoEnrollTeam(Long teamId);

    TournamentResponse removeTeam(Long tournamentId, Long teamId);

    /**
     * Qualifie les {@code count} premiers du tournoi {@code sourceTournamentId}
     * vers le tournoi cible. Si {@code targetTournamentId} est {@code null}, la
     * « Ligue des Champions » est utilisée. Les équipes déjà inscrites sont
     * ignorées. Retourne les noms des équipes nouvellement qualifiées.
     */
    List<String> qualifyTopTeams(Long sourceTournamentId, Long targetTournamentId, int count);

    List<TournamentResponse> getTournamentsByStatus(TournamentStatus status);
}
