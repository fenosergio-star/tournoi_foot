package com.example.tournament.util;

import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.Tournament;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchGeneratorUtilTest {

    private Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Test");
        tournament.setMaxTeams(64);
        return tournament;
    }

    private List<Team> teams(int count) {
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Team team = new Team();
            team.setId((long) (i + 1));
            team.setName("Équipe " + (i + 1));
            teams.add(team);
        }
        return teams;
    }

    @Test
    void championshipSimpleProducesRoundRobinWithoutDuplicates() {
        Tournament tournament = tournament();
        List<Team> teams = teams(4);

        List<Match> matches = MatchGeneratorUtil.generateChampionship(tournament, teams, false);

        assertThat(matches).hasSize(6);
        assertThat(matches.stream().map(Match::getRound).distinct().count()).isEqualTo(3);

        Set<String> pairs = new HashSet<>();
        for (Match match : matches) {
            String key = match.getHomeTeam().getId() + "-" + match.getAwayTeam().getId();
            assertThat(pairs.add(key)).as("match en double: " + key).isTrue();
        }
    }

    @Test
    void championshipAllerRetourDoublesTheMatches() {
        Tournament tournament = tournament();
        List<Team> teams = teams(4);

        List<Match> matches = MatchGeneratorUtil.generateChampionship(tournament, teams, true);

        assertThat(matches).hasSize(12);
        assertThat(matches.stream().map(Match::getRound).distinct().count()).isEqualTo(6);
    }

    @Test
    void championshipWithOddNumberOfTeamsAddsBye() {
        Tournament tournament = tournament();
        List<Team> teams = teams(5);

        List<Match> matches = MatchGeneratorUtil.generateChampionship(tournament, teams, false);

        assertThat(matches).hasSize(10);
        // 5 équipes => 5 journées, une équipe au repos par journée
        assertThat(matches.stream().map(Match::getRound).distinct().count()).isEqualTo(5);
    }

    @Test
    void knockoutWithNonPowerOfTwoUsesByes() {
        Tournament tournament = tournament();
        List<Team> teams = teams(10);

        List<Match> matches = MatchGeneratorUtil.generateKnockout(tournament, teams);

        // 16 équipes dans l'arbre => 2 matchs joués au 1er tour (10 - 8)
        long firstRoundRealMatches = matches.stream()
                .filter(m -> m.getRound() == 0)
                .filter(m -> m.getPhase() != MatchPhase.THIRD_PLACE)
                .count();
        assertThat(firstRoundRealMatches).isEqualTo(2);

        // 4 tours : ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, FINAL
        assertThat(matches.stream()
                .filter(m -> m.getPhase() != MatchPhase.THIRD_PLACE)
                .map(Match::getPhase)
                .distinct())
                .containsExactlyInAnyOrder(MatchPhase.ROUND_OF_16, MatchPhase.QUARTER_FINAL,
                        MatchPhase.SEMI_FINAL, MatchPhase.FINAL);

        // 6 exempts placés directement au tour suivant (8 places - 2 qualifiés)
        long roundOneFilled = matches.stream()
                .filter(m -> m.getRound() == 1)
                .flatMap(m -> java.util.stream.Stream.of(m.getHomeTeam(), m.getAwayTeam()))
                .filter(java.util.Objects::nonNull)
                .count();
        assertThat(roundOneFilled).isEqualTo(6);

        // Une petite finale est générée
        assertThat(matches).anyMatch(m -> m.getPhase() == MatchPhase.THIRD_PLACE);
    }

    @Test
    void knockoutWithThreeTeamsHasNoThirdPlaceMatch() {
        Tournament tournament = tournament();
        List<Team> teams = teams(3);

        List<Match> matches = MatchGeneratorUtil.generateKnockout(tournament, teams);

        // Un tournoi à 3 équipes ne compte qu'une seule demi-finale : pas de petite finale
        assertThat(matches).noneMatch(m -> m.getPhase() == MatchPhase.THIRD_PLACE);

        // L'exempt est directement placé en finale, l'autre place attend la demi-finale
        Match finalMatch = matches.stream()
                .filter(m -> m.getPhase() == MatchPhase.FINAL)
                .findFirst().orElseThrow();
        assertThat(finalMatch.getHomeTeam()).isNotNull();
        assertThat(finalMatch.getAwayTeam()).isNull();

        // Une seule demi-finale est jouée au premier tour
        long semis = matches.stream().filter(m -> m.getPhase() == MatchPhase.SEMI_FINAL).count();
        assertThat(semis).isEqualTo(1);
    }

    @Test
    void knockoutWithPowerOfTwoHasNoBye() {
        Tournament tournament = tournament();
        List<Team> teams = teams(8);

        List<Match> matches = MatchGeneratorUtil.generateKnockout(tournament, teams);

        long firstRoundMatches = matches.stream().filter(m -> m.getRound() == 0).count();
        assertThat(firstRoundMatches).isEqualTo(4);

        // 7 matchs + petite finale
        assertThat(matches).hasSize(8);
    }

    @Test
    void groupStageSplitsIntoGroupsOfFour() {
        Tournament tournament = tournament();
        List<Team> teams = teams(8);

        List<Match> matches = MatchGeneratorUtil.generateGroupStage(tournament, teams);

        // 2 poules de 4 => 12 matchs (aller-retour) chacune
        assertThat(matches).hasSize(24);
        assertThat(matches).allMatch(m -> m.getPhase() == MatchPhase.GROUP_STAGE);
        assertThat(matches.stream().map(Match::getGroup).distinct())
                .containsExactlyInAnyOrder("A", "B");
        assertThat(matches.stream().filter(m -> "A".equals(m.getGroup()))).hasSize(12);

        // Répartition déterministe : pas de tirage au sort (A = 4 premières, B = 4 suivantes)
        Set<Long> groupA = matches.stream().filter(m -> "A".equals(m.getGroup()))
                .flatMap(m -> java.util.stream.Stream.of(m.getHomeTeam().getId(), m.getAwayTeam().getId()))
                .collect(java.util.stream.Collectors.toSet());
        assertThat(groupA).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);

        // Chaque équipe affronte les 3 autres de SA poule en aller ET retour
        long matchesOfTeam1 = matches.stream()
                .filter(m -> m.getHomeTeam().getId() == 1L || m.getAwayTeam().getId() == 1L)
                .count();
        assertThat(matchesOfTeam1).isEqualTo(6);

        // Chaque paire de la poule A se rencontre exactement 2 fois (aller + retour)
        Map<String, Long> pairs = matches.stream()
                .filter(m -> "A".equals(m.getGroup()))
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> {
                            long a = m.getHomeTeam().getId();
                            long b = m.getAwayTeam().getId();
                            return Math.min(a, b) + "-" + Math.max(a, b);
                        },
                        java.util.stream.Collectors.counting()));
        assertThat(pairs).hasSize(6)
                .allSatisfy((pair, count) -> assertThat(count).isEqualTo(2L));
    }

    @Test
    void groupStageRejectsNonMultipleOfFour() {
        Tournament tournament = tournament();
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> MatchGeneratorUtil.generateGroupStage(tournament, teams(6))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void twoLeggedKnockoutHasDoubleLegsExceptFinal() {
        Tournament tournament = tournament();
        List<Team> qualified = teams(8);

        List<Match> matches = MatchGeneratorUtil.generateTwoLeggedKnockout(tournament, qualified);

        // Quarts : 4 confrontations × 2 = 8, demies : 2 × 2 = 4, finale : 1
        assertThat(matches).hasSize(13);

        Match finalMatch = matches.stream()
                .filter(m -> m.getPhase() == MatchPhase.FINAL)
                .findFirst().orElseThrow();
        assertThat(finalMatch.getLeg()).isNull();

        long quarterLegs = matches.stream().filter(m -> m.getPhase() == MatchPhase.QUARTER_FINAL).count();
        assertThat(quarterLegs).isEqualTo(8);

        // Chaque confrontation a bien une manche aller et une manche retour
        long leg1 = matches.stream().filter(m -> m.getLeg() != null && m.getLeg() == 1).count();
        long leg2 = matches.stream().filter(m -> m.getLeg() != null && m.getLeg() == 2).count();
        assertThat(leg1).isEqualTo(leg2).isEqualTo(6);

        // Le premier tour oppose bien 8 équipes (aucune attente)
        long firstRoundTeams = matches.stream()
                .filter(m -> m.getRound() == 1)
                .flatMap(m -> java.util.stream.Stream.of(m.getHomeTeam(), m.getAwayTeam()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        assertThat(firstRoundTeams).isEqualTo(8);
    }
}
