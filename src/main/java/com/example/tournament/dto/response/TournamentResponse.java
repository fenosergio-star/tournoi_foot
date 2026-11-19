package com.example.tournament.dto.response;

import com.example.tournament.entity.Country;

import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.TournamentType;
import java.time.LocalDate;

public class TournamentResponse {

    private Long id;
    private String name;
    private String sport;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;

    private Country country;
    private Integer maxTeams;
    private Integer registeredTeams;
    private TournamentType type;
    private TournamentStatus status;
    private Integer pointsWin;
    private Integer pointsDraw;
    private Integer pointsLoss;
    private String tiebreakers;

    public TournamentResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Integer getRegisteredTeams() { return registeredTeams; }
    public void setRegisteredTeams(Integer registeredTeams) { this.registeredTeams = registeredTeams; }

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
