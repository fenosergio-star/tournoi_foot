package com.example.tournament.service.impl;

import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.entity.*;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.StandingMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.StandingRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.RankingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RankingServiceImpl implements RankingService {

    private final StandingRepository standingRepository;
    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final StandingMapper standingMapper;

    public RankingServiceImpl(StandingRepository standingRepository,
                               MatchRepository matchRepository,
                               TournamentRepository tournamentRepository,
                               StandingMapper standingMapper) {
        this.standingRepository = standingRepository;
        this.matchRepository = matchRepository;
        this.tournamentRepository = tournamentRepository;
        this.standingMapper = standingMapper;
    }

    @Override
    @Transactional
    public List<StandingResponse> getRankingByTournament(Long tournamentId) {
        List<Standing> standings = ensureStandings(tournamentId);
        // En format poules, regroupe par poule avant d'attribuer les positions.
        if (standings.stream().anyMatch(s -> s.getGroup() != null)) {
            standings.sort((a, b) -> {
                String ga = a.getGroup() == null ? "" : a.getGroup();
                String gb = b.getGroup() == null ? "" : b.getGroup();
                return ga.compareTo(gb);
            });
        }
        assignPositions(standings);
        return standings.stream().map(standingMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recalculateRanking(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId));

        List<Standing> standings = ensureStandings(tournamentId);
        if (standings.isEmpty()) return;

        boolean poules = tournament.getType() == TournamentType.POULES_ELIMINATION;
        int pointsWin = safe(tournament.getPointsWin(), 3);
        int pointsDraw = safe(tournament.getPointsDraw(), 1);
        int pointsLoss = safe(tournament.getPointsLoss(), 0);

        for (Standing s : standings) {
            s.setMatchesPlayed(0);
            s.setWins(0);
            s.setDraws(0);
            s.setLosses(0);
            s.setGoalsFor(0);
            s.setGoalsAgainst(0);
            s.setPoints(0);
        }

        Map<Long, Standing> standingMap = standings.stream()
                .collect(Collectors.toMap(s -> s.getTeam().getId(), s -> s));

        List<Match> allTournamentMatches = matchRepository.findByTournamentId(tournamentId);
        List<Match> completedMatches = allTournamentMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                .filter(m -> m.getHomeTeam() != null && m.getAwayTeam() != null)
                // En format poules, seuls les matchs de groupes comptent pour le classement.
                .filter(m -> !poules || m.getPhase() == MatchPhase.GROUP_STAGE)
                .collect(Collectors.toList());

        // Renseigne la poule de chaque classement à partir des matchs générés.
        if (poules) {
            Map<Long, String> groupByTeam = new java.util.LinkedHashMap<>();
            for (Match m : allTournamentMatches) {
                if (m.getPhase() != MatchPhase.GROUP_STAGE || m.getGroup() == null) continue;
                if (m.getHomeTeam() != null) groupByTeam.put(m.getHomeTeam().getId(), m.getGroup());
                if (m.getAwayTeam() != null) groupByTeam.put(m.getAwayTeam().getId(), m.getGroup());
            }
            for (Standing s : standings) {
                if (s.getGroup() == null) s.setGroup(groupByTeam.get(s.getTeam().getId()));
            }
        }

        for (Match match : completedMatches) {
            Standing homeStanding = standingMap.get(match.getHomeTeam().getId());
            Standing awayStanding = standingMap.get(match.getAwayTeam().getId());
            if (homeStanding == null || awayStanding == null) continue;

            int homeGoals = safe(match.getHomeScore(), 0);
            int awayGoals = safe(match.getAwayScore(), 0);

            if (homeGoals > awayGoals) {
                homeStanding.recordWin(homeGoals, awayGoals);
                awayStanding.recordLoss(awayGoals, homeGoals);
            } else if (homeGoals < awayGoals) {
                awayStanding.recordWin(awayGoals, homeGoals);
                homeStanding.recordLoss(homeGoals, awayGoals);
            } else {
                homeStanding.recordDraw(homeGoals, awayGoals);
                awayStanding.recordDraw(awayGoals, homeGoals);
            }
        }

        // Attribution des points selon la configuration du tournoi
        for (Standing s : standings) {
            int points = s.getWins() * pointsWin + s.getDraws() * pointsDraw + s.getLosses() * pointsLoss;
            s.setPoints(points);
        }

        List<RankingCriterion> criteria = tournament.getTiebreakerCriteria();
        if (poules) {
            // Classement indépendant par poule.
            standings.sort((a, b) -> {
                String ga = a.getGroup() == null ? "" : a.getGroup();
                String gb = b.getGroup() == null ? "" : b.getGroup();
                int byGroup = ga.compareTo(gb);
                return byGroup != 0 ? byGroup : compare(a, b, criteria, completedMatches, pointsWin, pointsDraw, pointsLoss);
            });
        } else {
            standings.sort((a, b) -> compare(a, b, criteria, completedMatches, pointsWin, pointsDraw, pointsLoss));
        }

        assignPositions(standings);

        standingRepository.saveAll(standings);
    }

    /** Position au sein de la poule (ou classement global si pas de poules). */
    private void assignPositions(List<Standing> standings) {
        String currentGroup = null;
        int index = 0;
        boolean grouped = standings.stream().anyMatch(s -> s.getGroup() != null);
        for (int i = 0; i < standings.size(); i++) {
            Standing s = standings.get(i);
            String group = s.getGroup();
            if (grouped) {
                if (group == null ? currentGroup != null : !group.equals(currentGroup)) {
                    currentGroup = group;
                    index = 0;
                }
            }
            s.setPosition(++index);
        }
    }

    private List<Standing> ensureStandings(Long tournamentId) {
        List<Standing> standings = standingRepository.findByTournamentIdOrderByPositionAsc(tournamentId);
        if (!standings.isEmpty()) {
            return standings;
        }
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId));

        List<Standing> created = new ArrayList<>();
        for (Team team : tournament.getTeams()) {
            created.add(standingRepository.save(new Standing(tournament, team)));
        }
        return created;
    }

    /**
     * Comparateur de classement : points puis critères de départage configurables.
     */
    private int compare(Standing a, Standing b, List<RankingCriterion> criteria,
                        List<Match> matches, int pointsWin, int pointsDraw, int pointsLoss) {
        int points = Integer.compare(b.getPoints(), a.getPoints());
        if (points != 0) return points;

        for (RankingCriterion criterion : criteria) {
            int result = switch (criterion) {
                case WINS -> Integer.compare(b.getWins(), a.getWins());
                case GOAL_DIFFERENCE -> Integer.compare(b.getGoalDifference(), a.getGoalDifference());
                case GOALS_FOR -> Integer.compare(b.getGoalsFor(), a.getGoalsFor());
                case GOALS_AGAINST -> Integer.compare(a.getGoalsAgainst(), b.getGoalsAgainst());
                case HEAD_TO_HEAD -> headToHead(a, b, matches, pointsWin, pointsDraw, pointsLoss);
            };
            if (result != 0) return result;
        }

        // Dernier départage déterministe : nom d'équipe
        return a.getTeam().getName().compareToIgnoreCase(b.getTeam().getName());
    }

    /**
     * Confrontation directe : compare les résultats des matchs joués entre les
     * deux équipes (points, différence de buts, buts marqués).
     */
    private int headToHead(Standing a, Standing b, List<Match> matches,
                           int pointsWin, int pointsDraw, int pointsLoss) {
        int pointsA = 0, pointsB = 0;
        int forA = 0, againstA = 0, forB = 0, againstB = 0;

        for (Match match : matches) {
            Long homeId = match.getHomeTeam().getId();
            Long awayId = match.getAwayTeam().getId();
            boolean confrontation =
                    (homeId.equals(a.getTeam().getId()) && awayId.equals(b.getTeam().getId()))
                    || (homeId.equals(b.getTeam().getId()) && awayId.equals(a.getTeam().getId()));
            if (!confrontation) continue;

            int homeGoals = safe(match.getHomeScore(), 0);
            int awayGoals = safe(match.getAwayScore(), 0);
            boolean aIsHome = homeId.equals(a.getTeam().getId());
            int goalsA = aIsHome ? homeGoals : awayGoals;
            int goalsB = aIsHome ? awayGoals : homeGoals;

            forA += goalsA;
            againstA += goalsB;
            forB += goalsB;
            againstB += goalsA;

            if (goalsA > goalsB) {
                pointsA += pointsWin;
                pointsB += pointsLoss;
            } else if (goalsA < goalsB) {
                pointsB += pointsWin;
                pointsA += pointsLoss;
            } else {
                pointsA += pointsDraw;
                pointsB += pointsDraw;
            }
        }

        int byPoints = Integer.compare(pointsB, pointsA);
        if (byPoints != 0) return byPoints;
        int diffA = forA - againstA;
        int diffB = forB - againstB;
        int byDiff = Integer.compare(diffB, diffA);
        if (byDiff != 0) return byDiff;
        return Integer.compare(forB, forA);
    }

    private int safe(Integer value, int fallback) {
        return value != null ? value : fallback;
    }
}
