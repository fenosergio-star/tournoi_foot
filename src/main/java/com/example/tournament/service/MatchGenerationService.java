package com.example.tournament.service;

import com.example.tournament.dto.response.MatchResponse;

import java.util.List;

public interface MatchGenerationService {

    List<MatchResponse> generateChampionshipMatches(Long tournamentId);

    List<MatchResponse> generateKnockoutMatches(Long tournamentId);

    /**
     * Phase de groupes d'un tournoi {@code POULES_ELIMINATION} : répartit les
     * équipes en poules de 4 et génère un championnat aller-retour par poule.
     */
    List<MatchResponse> generateGroupStageMatches(Long tournamentId);

    /**
     * Phase finale d'un tournoi {@code POULES_ELIMINATION} : qualifie les deux
     * premiers de chaque poule et génère l'arbre d'élimination directe en
     * aller-retour (finale en match unique). À appeler une fois tous les
     * matchs de groupes terminés.
     */
    List<MatchResponse> generateEliminationPhase(Long tournamentId);
}
