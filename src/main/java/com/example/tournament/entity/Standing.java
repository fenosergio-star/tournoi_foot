package com.example.tournament.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "standings")
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    private Integer matchesPlayed = 0;
    private Integer wins = 0;
    private Integer draws = 0;
    private Integer losses = 0;
    private Integer goalsFor = 0;
    private Integer goalsAgainst = 0;
    private Integer points = 0;

    @Column(nullable = false)
    private Integer position = 0;

    /**
     * Lettre de la poule (A, B, C...) pour les tournois
     * {@code POULES_ELIMINATION}. Null = classement global.
     * Nom de colonne distinct de "group" (mot réservé MySQL).
     */
    @Column(name = "poule")
    private String group;

    public Standing() {
    }

    public Standing(Tournament tournament, Team team) {
        this.tournament = tournament;
        this.team = team;
    }

    // Les points sont attribués par le service de classement selon la
    // configuration du tournoi (victoire / nul / défaite).

    public void recordWin(int goalsFor, int goalsAgainst) {
        matchesPlayed++;
        wins++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
    }

    public void recordDraw(int goalsFor, int goalsAgainst) {
        matchesPlayed++;
        draws++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
    }

    public void recordLoss(int goalsFor, int goalsAgainst) {
        matchesPlayed++;
        losses++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
    }

    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public Integer getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(Integer matchesPlayed) { this.matchesPlayed = matchesPlayed; }

    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }

    public Integer getDraws() { return draws; }
    public void setDraws(Integer draws) { this.draws = draws; }

    public Integer getLosses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }

    public Integer getGoalsFor() { return goalsFor; }
    public void setGoalsFor(Integer goalsFor) { this.goalsFor = goalsFor; }

    public Integer getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(Integer goalsAgainst) { this.goalsAgainst = goalsAgainst; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
}
