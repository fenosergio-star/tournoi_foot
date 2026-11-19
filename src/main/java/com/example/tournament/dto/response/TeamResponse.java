package com.example.tournament.dto.response;

import com.example.tournament.entity.Country;

import com.example.tournament.entity.TeamStatus;
import java.util.List;

public class TeamResponse {

    private Long id;
    private String name;
    private String logo;
    private String city;
    private String coach;

    private Country country;
    private TeamStatus status;
    private Integer playerCount;
    private List<Long> tournamentIds;

    public TeamResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCoach() { return coach; }
    public void setCoach(String coach) { this.coach = coach; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public TeamStatus getStatus() { return status; }
    public void setStatus(TeamStatus status) { this.status = status; }

    public Integer getPlayerCount() { return playerCount; }
    public void setPlayerCount(Integer playerCount) { this.playerCount = playerCount; }

    public List<Long> getTournamentIds() { return tournamentIds; }
    public void setTournamentIds(List<Long> tournamentIds) { this.tournamentIds = tournamentIds; }
}
