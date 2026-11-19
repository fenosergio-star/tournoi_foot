package com.example.tournament.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Représentation de l'arbre d'élimination directe, organisée par tour.
 */
public class BracketResponse {

    private Long tournamentId;
    private String tournamentName;
    private List<BracketRoundResponse> rounds = new ArrayList<>();

    public BracketResponse() {
    }

    public BracketResponse(Long tournamentId, String tournamentName) {
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
    }

    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }

    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }

    public List<BracketRoundResponse> getRounds() { return rounds; }
    public void setRounds(List<BracketRoundResponse> rounds) { this.rounds = rounds; }
}
