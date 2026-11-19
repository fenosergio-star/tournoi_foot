package com.example.tournament.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String sport;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private String location;

    @Enumerated(EnumType.STRING)
    private Country country;

    private Integer maxTeams;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status;

    @Column(nullable = false)
    private Integer pointsWin = 3;

    @Column(nullable = false)
    private Integer pointsDraw = 1;

    @Column(nullable = false)
    private Integer pointsLoss = 0;

    /**
     * Ordre des critères de départage, sous forme de liste CSV d'énumérations
     * {@link RankingCriterion}. Exemple : "GOAL_DIFFERENCE,GOALS_FOR,HEAD_TO_HEAD".
     */
    @Column(length = 255)
    private String tiebreakers = "GOAL_DIFFERENCE,GOALS_FOR,HEAD_TO_HEAD";

    @ManyToMany
    @JoinTable(
        name = "tournament_teams",
        joinColumns = @JoinColumn(name = "tournament_id"),
        inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    @OrderBy("id ASC")
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Match> matches = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Standing> standings = new ArrayList<>();

    public Tournament() {
        this.status = TournamentStatus.BROUILLON;
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

    /**
     * Convertit la liste CSV de critères en liste d'énumérations, en ignorant
     * les valeurs vides ou inconnues.
     */
    public List<RankingCriterion> getTiebreakerCriteria() {
        List<RankingCriterion> criteria = new ArrayList<>();
        if (tiebreakers == null || tiebreakers.isBlank()) {
            return criteria;
        }
        for (String raw : tiebreakers.split(",")) {
            String value = raw.trim();
            if (value.isEmpty()) continue;
            try {
                criteria.add(RankingCriterion.valueOf(value.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // critère inconnu ignoré
            }
        }
        return criteria;
    }

    public List<Team> getTeams() { return teams; }
    public void setTeams(List<Team> teams) { this.teams = teams; }

    public List<Match> getMatches() { return matches; }
    public void setMatches(List<Match> matches) { this.matches = matches; }

    public List<Standing> getStandings() { return standings; }
    public void setStandings(List<Standing> standings) { this.standings = standings; }

    public void addTeam(Team team) {
        teams.add(team);
        team.getTournaments().add(this);
    }

    public void removeTeam(Team team) {
        teams.remove(team);
        team.getTournaments().remove(this);
    }

    public boolean isTeamRegistered(Team team) {
        return teams.contains(team);
    }

    public int getTeamCount() {
        return teams != null ? teams.size() : 0;
    }

    public boolean isFull() {
        return maxTeams != null && teams.size() >= maxTeams;
    }
}
