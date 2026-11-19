package com.example.tournament.entity;

public enum TournamentType {
    CHAMPIONNAT_SIMPLE,
    CHAMPIONNAT_ALLER_RETOUR,
    ELIMINATION_DIRECTE,
    /**
     * Format Ligue des Champions : phase de poules (championnat aller-retour
     * par poule) puis élimination directe en aller-retour pour les qualifiés.
     */
    POULES_ELIMINATION
}
