package com.example.tournament.dto.response;

import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public class MatchResponse {

    private Long id;
    private Long tournamentId;
    private String tournamentName;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;
    private LocalDate matchDate;
    private LocalTime matchTime;
    private String field;
    private MatchPhase phase;
    private MatchStatus status;
    private Integer homeScore;
    private Integer awayScore;
    private Integer homePenalties;
    private Integer awayPenalties;
    private Integer round;
    private Integer bracketPosition;
    private String winnerName;
    private String phaseLabel;
    private boolean teamsReady;
    /** Lettre de la poule (A, B...) pour les phases de groupes. */
    private String group;
    /** Manche d'un aller-retour : 1 = aller, 2 = retour. Null = aller simple. */
    private Integer leg;

    public MatchResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }

    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }

    public Long getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(Long homeTeamId) { this.homeTeamId = homeTeamId; }

    public String getHomeTeamName() { return homeTeamName; }
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    public Long getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getAwayTeamName() { return awayTeamName; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

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

    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }

    public String getPhaseLabel() { return phaseLabel; }
    public void setPhaseLabel(String phaseLabel) { this.phaseLabel = phaseLabel; }

    public boolean isTeamsReady() { return teamsReady; }
    public void setTeamsReady(boolean teamsReady) { this.teamsReady = teamsReady; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public Integer getLeg() { return leg; }
    public void setLeg(Integer leg) { this.leg = leg; }
}
