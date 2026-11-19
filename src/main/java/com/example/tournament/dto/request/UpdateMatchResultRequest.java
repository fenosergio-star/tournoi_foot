package com.example.tournament.dto.request;

import jakarta.validation.constraints.*;

public class UpdateMatchResultRequest {

    @NotNull(message = "Le score de l'équipe domicile est obligatoire")
    @Min(value = 0, message = "Le score ne peut pas être négatif")
    private Integer homeScore;

    @NotNull(message = "Le score de l'équipe visiteuse est obligatoire")
    @Min(value = 0, message = "Le score ne peut pas être négatif")
    private Integer awayScore;

    @Min(value = 0, message = "Le nombre de tirs au but ne peut pas être négatif")
    private Integer homePenalties;

    @Min(value = 0, message = "Le nombre de tirs au but ne peut pas être négatif")
    private Integer awayPenalties;

    public UpdateMatchResultRequest() {
    }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public Integer getHomePenalties() { return homePenalties; }
    public void setHomePenalties(Integer homePenalties) { this.homePenalties = homePenalties; }

    public Integer getAwayPenalties() { return awayPenalties; }
    public void setAwayPenalties(Integer awayPenalties) { this.awayPenalties = awayPenalties; }
}
