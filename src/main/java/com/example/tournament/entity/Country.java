package com.example.tournament.entity;

/**
 * Pays rattachant une équipe ou un tournoi à son championnat national.
 * Un tournoi sans pays (null) est international (ex : Ligue des Champions).
 */
public enum Country {
    FRANCE("France"),
    ENGLAND("Angleterre"),
    SPAIN("Espagne"),
    ITALY("Italie"),
    GERMANY("Allemagne");

    private final String label;

    Country(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
