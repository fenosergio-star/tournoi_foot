package com.example.tournament.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    private LocalDate matchDate;

    private LocalTime matchTime;

    private String field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private Integer homeScore;

    private Integer awayScore;

    /** Tirs au but de l'équipe domicile (élimination directe en cas d'égalité). */
    private Integer homePenalties;

    /** Tirs au but de l'équipe visiteuse (élimination directe en cas d'égalité). */
    private Integer awayPenalties;

    private Integer round;

    /** Position du match dans son tour, utilisée pour construire l'arbre d'élimination. */
    private Integer bracketPosition;

    /**
     * Lettre de la poule (A, B, C...) pour les phases de groupes des tournois
     * {@code POULES_ELIMINATION}. Null pour les autres types de tournoi.
     * Nom de colonne distinct de "group" (mot réservé MySQL).
     */
    @Column(name = "poule")
    private String group;

    /**
     * Manche d'une confrontation aller-retour en élimination directe :
     * 1 = match aller, 2 = match retour. Null pour les matchs en aller simple.
     */
    private Integer leg;

    @Column(name = "is_generated", nullable = false)
    private Boolean generated = false;

    public Match() {
        this.status = MatchStatus.PLANNED;
        this.homeScore = 0;
        this.awayScore = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public Team getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }

    public Team getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }

    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }

    public LocalTime getMatchTime() { return matchTime; }
    public void setMatchTime(LocalTime matchTime) { this.matchTime = matchTime; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public MatchPhase getPhase() { return phase; }
    public void setPhase(MatchPhase phase) { this.phase = phase; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public Integer getHomePenalties() { return homePenalties; }
    public void setHomePenalties(Integer homePenalties) { this.homePenalties = homePenalties; }

    public Integer getAwayPenalties() { return awayPenalties; }
    public void setAwayPenalties(Integer awayPenalties) { this.awayPenalties = awayPenalties; }

    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }

    public Integer getBracketPosition() { return bracketPosition; }
    public void setBracketPosition(Integer bracketPosition) { this.bracketPosition = bracketPosition; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public Integer getLeg() { return leg; }
    public void setLeg(Integer leg) { this.leg = leg; }

    public Boolean getGenerated() { return generated; }
    public void setGenerated(Boolean generated) { this.generated = generated; }

    public boolean isKnockout() {
        return phase != null && phase != MatchPhase.GROUP_STAGE;
    }

    /**
     * Détermine le vainqueur du match. En cas d'égalité, les tirs au but
     * départagent les rencontres à élimination directe.
     */
    public Team getWinner() {
        if (homeScore == null || awayScore == null) return null;
        if (homeScore > awayScore) return homeTeam;
        if (awayScore > homeScore) return awayTeam;
        if (homePenalties != null && awayPenalties != null) {
            if (homePenalties > awayPenalties) return homeTeam;
            if (awayPenalties > homePenalties) return awayTeam;
        }
        return null;
    }

    public Team getLoser() {
        Team winner = getWinner();
        if (winner == null) return null;
        return winner.equals(homeTeam) ? awayTeam : homeTeam;
    }

    public boolean isDraw() {
        if (homeScore == null || awayScore == null) return false;
        return homeScore.equals(awayScore);
    }
}
