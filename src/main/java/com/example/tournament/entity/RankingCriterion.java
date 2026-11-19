package com.example.tournament.entity;

/**
 * Critères de départage configurables pour le classement d'un championnat.
 * Le classement est d'abord trié par points, puis par les critères listés dans
 * l'ordre défini sur le tournoi.
 */
public enum RankingCriterion {
    WINS,
    GOAL_DIFFERENCE,
    GOALS_FOR,
    GOALS_AGAINST,
    HEAD_TO_HEAD
}
