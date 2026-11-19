package com.example.tournament.util;

import com.example.tournament.entity.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MatchGeneratorUtil {

    private MatchGeneratorUtil() {}

    /**
     * Round-Robin (méthode de Berger). Produit des journées équilibrées où chaque
     * équipe joue exactement une fois par journée, sans doublon.
     * <p>
     * Pour un aller-retour, les matchs retour inversent domicile/extérieur et
     * poursuivent la numérotation des journées.
     */
    public static List<Match> generateChampionship(Tournament tournament, List<Team> teams, boolean allerRetour) {
        List<Match> matches = new ArrayList<>();
        List<Team> list = new ArrayList<>(teams);

        boolean hasBye = list.size() % 2 != 0;
        if (hasBye) {
            list.add(null);
        }

        int n = list.size();
        int half = n / 2;
        int roundsPerLeg = n - 1;

        // Aller
        List<Team> workingList = new ArrayList<>(list);
        for (int r = 0; r < roundsPerLeg; r++) {
            for (int i = 0; i < half; i++) {
                Team home = workingList.get(i);
                Team away = workingList.get(n - 1 - i);
                if (home != null && away != null) {
                    matches.add(createGeneratedMatch(tournament, home, away, r + 1,
                            MatchPhase.GROUP_STAGE, matches.size()));
                }
            }
            workingList.add(1, workingList.remove(workingList.size() - 1));
        }

        // Retour
        if (allerRetour) {
            for (int r = 0; r < roundsPerLeg; r++) {
                for (int i = 0; i < half; i++) {
                    Team away = workingList.get(i);
                    Team home = workingList.get(n - 1 - i);
                    if (home != null && away != null) {
                        matches.add(createGeneratedMatch(tournament, home, away,
                                roundsPerLeg + r + 1, MatchPhase.GROUP_STAGE, matches.size()));
                    }
                }
                workingList.add(1, workingList.remove(workingList.size() - 1));
            }
        }

        return matches;
    }

    /**
     * Phase de groupes (format Ligue des Champions) : répartit les équipes en
     * poules de 4 et génère pour chaque poule un championnat aller-retour
     * (méthode de Berger), donc chaque équipe affronte toutes les autres de sa
     * poule en match aller ET retour. Chaque match porte la lettre de sa poule.
     * <p>
     * La répartition est déterministe et logique : les équipes sont placées dans
     * les poules dans leur ordre d'inscription (poule A = 4 premières, etc.).
     * Aucun tirage au sort n'est effectué.
     * <p>
     * Le nombre d'équipes doit être un multiple de 4.
     */
    public static List<Match> generateGroupStage(Tournament tournament, List<Team> teams) {
        if (teams == null || teams.size() < 4 || teams.size() % 4 != 0) {
            throw new IllegalArgumentException(
                "La phase de groupes requiert un nombre d'équipes multiple de 4 (actuellement : "
                    + (teams == null ? 0 : teams.size()) + ")");
        }

        List<Match> matches = new ArrayList<>();
        int groupCount = teams.size() / 4;
        for (int g = 0; g < groupCount; g++) {
            String letter = String.valueOf((char) ('A' + g));
            List<Team> groupTeams = new ArrayList<>(teams.subList(g * 4, (g + 1) * 4));
            List<Match> groupMatches = generateChampionship(tournament, groupTeams, true);
            for (Match match : groupMatches) {
                match.setGroup(letter);
            }
            matches.addAll(groupMatches);
        }
        return matches;
    }

    /**
     * Élimination directe en confrontation aller-retour (format Ligue des
     * Champions) : les tours intermédiaires sont joués en deux manches (score
     * cumulé), la finale est un match unique. L'arbre des tours suivants est
     * créé en avance avec des matchs « placeholder » dont les équipes seront
     * attribuées à mesure que les confrontations se décident.
     *
     * @param qualified les qualifiés par ordre de tête de série : alternance
     *                  vainqueur de poule / deuxième de poule, de sorte qu'une
     *                  tête de série ne rencontre jamais un autre qualifié de
     *                  sa propre poule au premier tour.
     */
    public static List<Match> generateTwoLeggedKnockout(Tournament tournament, List<Team> qualified) {
        int teamCount = qualified == null ? 0 : qualified.size();
        if (teamCount < 4 || Integer.bitCount(teamCount) != 1) {
            throw new IllegalArgumentException(
                "L'élimination directe requiert un nombre de qualifiés puissance de 2 (actuellement : "
                    + teamCount + ")");
        }

        List<Match> matches = new ArrayList<>();
        int numberOfRounds = Integer.numberOfTrailingZeros(teamCount);

        // Tous les matchs placeholder, sauf le premier tour (rempli ci-dessous)
        // et la finale (match unique, placeholder seul).
        Map<Integer, List<Match>> tiesByRound = new LinkedHashMap<>();
        for (int round = 1; round <= numberOfRounds; round++) {
            int teamsInRound = teamCount >> (round - 1);
            MatchPhase phase = PhaseLabels.phaseForTeamCount(teamsInRound);
            boolean isFinal = teamsInRound == 2;
            int tieCount = teamsInRound / 2;
            List<Match> roundMatches = new ArrayList<>();
            for (int position = 0; position < tieCount; position++) {
                if (isFinal) {
                    Match finalMatch = createPlaceholderMatch(tournament, round, phase, position);
                    roundMatches.add(finalMatch);
                    matches.add(finalMatch);
                } else {
                    Match leg1 = createPlaceholderMatch(tournament, round, phase, position);
                    leg1.setLeg(1);
                    Match leg2 = createPlaceholderMatch(tournament, round, phase, position);
                    leg2.setLeg(2);
                    roundMatches.add(leg1);
                    roundMatches.add(leg2);
                    matches.add(leg1);
                    matches.add(leg2);
                }
            }
            tiesByRound.put(round, roundMatches);
        }

        // Premier tour : rencontre tête de série (vainqueur de poule) contre
        // deuxième d'une autre poule, en aller-retour.
        List<Match> firstRound = tiesByRound.get(1);
        int firstRoundTies = firstRound.size() / 2;
        for (int position = 0; position < firstRoundTies; position++) {
            Team home = qualified.get(position * 2);
            Team away = qualified.get(position * 2 + 1);
            final int tiePosition = position;
            List<Match> tie = firstRound.stream()
                    .filter(m -> m.getBracketPosition() != null && m.getBracketPosition() == tiePosition)
                    .toList();
            for (Match leg : tie) {
                if (leg.getLeg() != null && leg.getLeg() == 1) {
                    leg.setHomeTeam(home);
                    leg.setAwayTeam(away);
                } else {
                    leg.setHomeTeam(away);
                    leg.setAwayTeam(home);
                }
            }
        }

        return matches;
    }

    /**
     * Élimination directe : génère l'intégralité de l'arbre (tous les tours),
     * en gérant les exempts (byes) lorsque le nombre d'équipes n'est pas une
     * puissance de 2. Les équipes exemptées sont directement placées au tour
     * suivant. Les matchs des tours futurs sont créés avec des équipes nulles
     * (TBD) et seront complétés au fur et à mesure des résultats.
     * <p>
     * L'appariement suit l'ordre d'inscription des équipes, sans tirage au sort.
     * <p>
     * Une petite finale est ajoutée pour les tournois d'au moins 4 équipes.
     */
    public static List<Match> generateKnockout(Tournament tournament, List<Team> teams) {
        List<Match> matches = new ArrayList<>();
        int teamCount = teams.size();

        int bracketSize = 1;
        while (bracketSize < teamCount) {
            bracketSize *= 2;
        }
        int numberOfRounds = Integer.numberOfTrailingZeros(bracketSize);

        List<Team> seeded = new ArrayList<>(teams);

        // 1) Créer les matchs "placeholder" de tous les tours (positions complètes)
        Map<Integer, List<Match>> matchesByRound = new LinkedHashMap<>();
        for (int round = 1; round < numberOfRounds; round++) {
            int count = bracketSize >> (round + 1);
            int teamsInRound = bracketSize >> round;
            MatchPhase phase = PhaseLabels.phaseForTeamCount(teamsInRound);
            List<Match> roundMatches = new ArrayList<>();
            for (int position = 0; position < count; position++) {
                Match match = createPlaceholderMatch(tournament, round, phase, position);
                roundMatches.add(match);
                matches.add(match);
            }
            matchesByRound.put(round, roundMatches);
        }

        // 2) Construire le premier tour et propager les exempts
        int byeCount = bracketSize - teamCount;
        int firstRoundMatchCount = bracketSize / 2;
        int teamIndex = 0;

        for (int position = 0; position < firstRoundMatchCount; position++) {
            Team home;
            Team away;
            if (position < byeCount) {
                home = seeded.isEmpty() ? null : seeded.get(teamIndex++);
                away = null;
            } else {
                home = seeded.get(teamIndex++);
                away = seeded.get(teamIndex++);
            }

            if (home != null && away != null) {
                Match firstRoundMatch = createGeneratedMatch(tournament, home, away, 0,
                        PhaseLabels.phaseForTeamCount(bracketSize), position);
                matches.add(firstRoundMatch);
            } else if (home != null || away != null) {
                Team byeTeam = home != null ? home : away;
                propagateFirstRoundWinner(matchesByRound, numberOfRounds, position, byeTeam);
            }
        }

        // 3) Petite finale : uniquement si deux demi-finales réelles existent.
        //    Un tournoi à 3 équipes (arbre de 4) n'a qu'une seule demi-finale.
        boolean thirdPlaceRelevant = numberOfRounds >= 2 && !(bracketSize == 4 && byeCount > 0);
        if (thirdPlaceRelevant) {
            Match thirdPlace = new Match();
            thirdPlace.setTournament(tournament);
            thirdPlace.setPhase(MatchPhase.THIRD_PLACE);
            thirdPlace.setRound(numberOfRounds - 1);
            thirdPlace.setStatus(MatchStatus.PLANNED);
            thirdPlace.setGenerated(true);
            matches.add(thirdPlace);
        }

        return matches;
    }

    /**
     * Place l'équipe exemptée dans le match du tour suivant.
     * Le match au tour {@code round} et à la position {@code position} alimente
     * le match de position {@code position / 2} du tour suivant (domicile si la
     * position est paire, extérieur sinon). Comme il n'existe pas de match au
     * premier tour pour un exempt, on place directement l'équipe au tour 1.
     */
    private static void propagateFirstRoundWinner(Map<Integer, List<Match>> matchesByRound,
                                                   int numberOfRounds, int firstRoundPosition, Team team) {
        if (numberOfRounds <= 1) {
            return;
        }
        int nextRound = 1;
        int nextPosition = firstRoundPosition / 2;
        List<Match> nextRoundMatches = matchesByRound.get(nextRound);
        if (nextRoundMatches == null || nextPosition >= nextRoundMatches.size()) {
            return;
        }
        Match next = nextRoundMatches.get(nextPosition);
        if (firstRoundPosition % 2 == 0) {
            next.setHomeTeam(team);
        } else {
            next.setAwayTeam(team);
        }
    }

    private static Match createGeneratedMatch(Tournament tournament, Team home, Team away,
                                              int round, MatchPhase phase, int bracketPosition) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setRound(round);
        match.setPhase(phase);
        match.setBracketPosition(bracketPosition);
        match.setGenerated(true);
        return match;
    }

    private static Match createPlaceholderMatch(Tournament tournament, int round,
                                                MatchPhase phase, int bracketPosition) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(round);
        match.setPhase(phase);
        match.setBracketPosition(bracketPosition);
        match.setGenerated(true);
        return match;
    }
}
