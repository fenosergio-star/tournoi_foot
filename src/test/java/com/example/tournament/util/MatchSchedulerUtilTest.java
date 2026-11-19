package com.example.tournament.util;

import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchSchedulerUtilTest {

    private Team team(long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("Équipe " + id);
        return team;
    }

    private Match match(Team home, Team away) {
        Match match = new Match();
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setPhase(MatchPhase.GROUP_STAGE);
        return match;
    }

    @Test
    void schedulerNeverPlaysSameTeamTwiceOnSameDay() {
        Team a = team(1);
        Team b = team(2);
        Team c = team(3);
        Team d = team(4);

        List<Match> matches = List.of(
                match(a, b),
                match(c, d),
                match(a, c),
                match(b, d),
                match(a, d),
                match(b, c)
        );

        MatchSchedulerUtil.schedule(matches, LocalDate.of(2026, 1, 1), 2);

        for (Match match : matches) {
            assertThat(match.getMatchDate()).isNotNull();
            assertThat(match.getMatchTime()).isNotNull();
            assertThat(match.getField()).isNotNull();
        }

        // Aucune équipe ne doit jouer deux fois le même jour
        for (LocalDate date : matches.stream().map(Match::getMatchDate).distinct().toList()) {
            Set<Long> playing = new HashSet<>();
            for (Match match : matches) {
                if (!match.getMatchDate().equals(date)) continue;
                assertThat(playing.add(match.getHomeTeam().getId())).isTrue();
                assertThat(playing.add(match.getAwayTeam().getId())).isTrue();
            }
        }
    }

    @Test
    void schedulerAvoidsFieldOverlap() {
        Team a = team(1);
        Team b = team(2);
        Team c = team(3);
        Team d = team(4);

        List<Match> matches = List.of(match(a, b), match(c, d));
        MatchSchedulerUtil.schedule(matches, LocalDate.of(2026, 1, 1), 1);

        Set<String> used = new HashSet<>();
        for (Match match : matches) {
            String key = match.getMatchDate() + "T" + match.getMatchTime() + "@" + match.getField();
            assertThat(used.add(key)).as("créneau en double: " + key).isTrue();
        }
    }

    @Test
    void hasConflictDetectsSameTeamAtSameTime() {
        Team a = team(1);
        Team b = team(2);
        Team c = team(3);

        Match existing = match(a, b);
        existing.setId(10L);
        existing.setMatchDate(LocalDate.of(2026, 1, 1));
        existing.setMatchTime(java.time.LocalTime.of(14, 0));
        existing.setField("Terrain 1");

        boolean conflict = MatchSchedulerUtil.hasConflict(
                a, c, LocalDate.of(2026, 1, 1), java.time.LocalTime.of(14, 0),
                "Terrain 2", List.of(existing), null);

        assertThat(conflict).isTrue();
    }

    @Test
    void hasConflictDetectsSameFieldAtSameTime() {
        Team a = team(1);
        Team b = team(2);
        Team c = team(3);
        Team d = team(4);

        Match existing = match(a, b);
        existing.setId(10L);
        existing.setMatchDate(LocalDate.of(2026, 1, 1));
        existing.setMatchTime(java.time.LocalTime.of(14, 0));
        existing.setField("Terrain 1");

        boolean conflict = MatchSchedulerUtil.hasConflict(
                c, d, LocalDate.of(2026, 1, 1), java.time.LocalTime.of(14, 0),
                "Terrain 1", List.of(existing), null);

        assertThat(conflict).isTrue();
    }
}
