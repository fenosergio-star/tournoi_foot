package com.example.tournament.util;

import com.example.tournament.entity.MatchPhase;

/**
 * Libellés français des phases de tournoi.
 */
public final class PhaseLabels {

    private PhaseLabels() {}

    public static String label(MatchPhase phase) {
        if (phase == null) return "";
        return switch (phase) {
            case GROUP_STAGE -> "Phase de groupes / Journée";
            case ROUND_OF_32 -> "Trente-deuxièmes de finale";
            case ROUND_OF_16 -> "Huitièmes de finale";
            case QUARTER_FINAL -> "Quarts de finale";
            case SEMI_FINAL -> "Demi-finales";
            case THIRD_PLACE -> "Petite finale";
            case FINAL -> "Finale";
        };
    }

    /**
     * Détermine la phase correspondant au nombre d'équipes engagées dans un tour.
     */
    public static MatchPhase phaseForTeamCount(int teamCount) {
        return switch (teamCount) {
            case 32 -> MatchPhase.ROUND_OF_32;
            case 16 -> MatchPhase.ROUND_OF_16;
            case 8 -> MatchPhase.QUARTER_FINAL;
            case 4 -> MatchPhase.SEMI_FINAL;
            case 2 -> MatchPhase.FINAL;
            default -> MatchPhase.GROUP_STAGE;
        };
    }
}
