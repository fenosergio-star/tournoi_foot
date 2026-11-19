package com.example.tournament.dto.response;

import com.example.tournament.entity.MatchPhase;

import java.util.ArrayList;
import java.util.List;

/**
 * Un tour de l'arbre d'élimination (huitièmes, quarts, demi-finales, finale...).
 */
public class BracketRoundResponse {

    private MatchPhase phase;
    private String label;
    private Integer round;
    private List<MatchResponse> matches = new ArrayList<>();

    public BracketRoundResponse() {
    }

    public BracketRoundResponse(MatchPhase phase, String label, Integer round) {
        this.phase = phase;
        this.label = label;
        this.round = round;
    }

    public MatchPhase getPhase() { return phase; }
    public void setPhase(MatchPhase phase) { this.phase = phase; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }

    public List<MatchResponse> getMatches() { return matches; }
    public void setMatches(List<MatchResponse> matches) { this.matches = matches; }
}
