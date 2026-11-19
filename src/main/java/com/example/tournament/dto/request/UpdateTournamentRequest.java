package com.example.tournament.dto.request;

import com.example.tournament.entity.TournamentType;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.Country;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class UpdateTournamentRequest {

    @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
    private String name;

    private String sport;

    private String description;

    @FutureOrPresent(message = "La date de début doit être aujourd'hui ou dans le futur")
    private LocalDate startDate;

    private LocalDate endDate;

    private String location;

    private Country country;

    @Min(value = 2, message = "Un tournoi doit accueillir au moins 2 équipes")
    private Integer maxTeams;

    private TournamentType type;

    private TournamentStatus status;

    @Min(value = 0, message = "Le nombre de points par victoire ne peut pas être négatif")
    private Integer pointsWin;

    @Min(value = 0, message = "Le nombre de points par match nul ne peut pas être négatif")
    private Integer pointsDraw;

    @Min(value = 0, message = "Le nombre de points par défaite ne peut pas être négatif")
    private Integer pointsLoss;

    private String tiebreakers;

    public UpdateTournamentRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public Integer getMaxTeams() { return maxTeams; }
    public void setMaxTeams(Integer maxTeams) { this.maxTeams = maxTeams; }

    public TournamentType getType() { return type; }
    public void setType(TournamentType type) { this.type = type; }

    public TournamentStatus getStatus() { return status; }
    public void setStatus(TournamentStatus status) { this.status = status; }

    public Integer getPointsWin() { return pointsWin; }
    public void setPointsWin(Integer pointsWin) { this.pointsWin = pointsWin; }

    public Integer getPointsDraw() { return pointsDraw; }
    public void setPointsDraw(Integer pointsDraw) { this.pointsDraw = pointsDraw; }

    public Integer getPointsLoss() { return pointsLoss; }
    public void setPointsLoss(Integer pointsLoss) { this.pointsLoss = pointsLoss; }

    public String getTiebreakers() { return tiebreakers; }
    public void setTiebreakers(String tiebreakers) { this.tiebreakers = tiebreakers; }
}
