package com.example.tournament.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTest {

    private Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private Match match(Team home, Team away) {
        Match match = new Match();
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        return match;
    }

    @Test
    void homeTeamWinsWithHigherScore() {
        Team home = team(1L, "Home");
        Team away = team(2L, "Away");
        Match match = match(home, away);
        match.setHomeScore(3);
        match.setAwayScore(1);

        assertThat(match.getWinner()).isEqualTo(home);
        assertThat(match.getLoser()).isEqualTo(away);
    }

    @Test
    void awayTeamWinsWithHigherScore() {
        Team home = team(1L, "Home");
        Team away = team(2L, "Away");
        Match match = match(home, away);
        match.setHomeScore(0);
        match.setAwayScore(2);

        assertThat(match.getWinner()).isEqualTo(away);
    }

    @Test
    void penaltiesDecideDrawInKnockout() {
        Team home = team(1L, "Home");
        Team away = team(2L, "Away");
        Match match = match(home, away);
        match.setHomeScore(1);
        match.setAwayScore(1);
        match.setHomePenalties(4);
        match.setAwayPenalties(3);

        assertThat(match.getWinner()).isEqualTo(home);
        assertThat(match.getLoser()).isEqualTo(away);
    }

    @Test
    void drawWithoutPenaltiesHasNoWinner() {
        Team home = team(1L, "Home");
        Team away = team(2L, "Away");
        Match match = match(home, away);
        match.setHomeScore(2);
        match.setAwayScore(2);

        assertThat(match.getWinner()).isNull();
        assertThat(match.getLoser()).isNull();
    }

    @Test
    void placeholderMatchWithoutTeamsHasNoWinner() {
        Match match = match(null, null);
        match.setHomeScore(0);
        match.setAwayScore(0);

        assertThat(match.getWinner()).isNull();
    }

    @Test
    void knockoutIsDetectedFromPhase() {
        Match group = new Match();
        group.setPhase(MatchPhase.GROUP_STAGE);
        assertThat(group.isKnockout()).isFalse();

        Match finalMatch = new Match();
        finalMatch.setPhase(MatchPhase.FINAL);
        assertThat(finalMatch.isKnockout()).isTrue();
    }
}
