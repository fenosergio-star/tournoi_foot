package com.example.tournament.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Demande de report / replanification d'un match.
 */
public class RescheduleMatchRequest {

    @NotNull(message = "La date du match est obligatoire")
    private LocalDate matchDate;

    @NotNull(message = "L'heure du match est obligatoire")
    private LocalTime matchTime;

    private String field;

    public RescheduleMatchRequest() {
    }

    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }

    public LocalTime getMatchTime() { return matchTime; }
    public void setMatchTime(LocalTime matchTime) { this.matchTime = matchTime; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
}
