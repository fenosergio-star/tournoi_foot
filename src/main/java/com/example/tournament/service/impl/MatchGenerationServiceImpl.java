package com.example.tournament.service.impl;

import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.*;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.StandingRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.MatchGenerationService;
import com.example.tournament.util.MatchGeneratorUtil;
import com.example.tournament.util.MatchSchedulerUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MatchGenerationServiceImpl implements MatchGenerationService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final StandingRepository standingRepository;
    private final MatchMapper matchMapper;

    public MatchGenerationServiceImpl(MatchRepository matchRepository,
                                       TournamentRepository tournamentRepository,
                                       StandingRepository standingRepository,
                                       MatchMapper matchMapper) {
        this.matchRepository = matchRepository;
        this.tournamentRepository = tournamentRepository;
        this.standingRepository = standingRepository;
        this.matchMapper = matchMapper;
    }

    @Override
    @Transactional
    public List<MatchResponse> generateChampionshipMatches(Long tournamentId) {
        Tournament tournament = loadTournamentForGeneration(tournamentId);
        List<Team> teams = tournament.getTeams();
        ensureMinimumTeams(teams, 2, "Il faut au moins 2 équipes pour générer les matchs");

        boolean allerRetour = tournament.getType() == TournamentType.CHAMPIONNAT_ALLER_RETOUR;
        List<Match> generatedMatches = MatchGeneratorUtil.generateChampionship(tournament, teams, allerRetour);
        MatchSchedulerUtil.schedule(generatedMatches, tournament.getStartDate(), fieldCount(teams.size()));

        matchRepository.saveAll(generatedMatches);
        initializeStandings(tournament);
        tournament.setStatus(TournamentStatus.PROGRAMME);
        tournamentRepository.save(tournament);

        return generatedMatches.stream().map(matchMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MatchResponse> generateKnockoutMatches(Long tournamentId) {
        Tournament tournament = loadTournamentForGeneration(tournamentId);
        List<Team> teams = tournament.getTeams();
        ensureMinimumTeams(teams, 2, "Il faut au moins 2 équipes pour générer un tournoi à élimination directe");

        List<Match> generatedMatches = MatchGeneratorUtil.generateKnockout(tournament, teams);
        MatchSchedulerUtil.schedule(generatedMatches, tournament.getStartDate(), fieldCount(teams.size()));

        matchRepository.saveAll(generatedMatches);
        tournament.setStatus(TournamentStatus.PROGRAMME);
        tournamentRepository.save(tournament);

        return generatedMatches.stream().map(matchMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MatchResponse> generateGroupStageMatches(Long tournamentId) {
        Tournament tournament = loadTournamentForGeneration(tournamentId);
        if (tournament.getType() != TournamentType.POULES_ELIMINATION) {
            throw new InvalidOperationException(
                "Le format poules n'est pas applicable à ce tournoi");
        }
        List<Team> teams = tournament.getTeams();
        ensureMinimumTeams(teams, 4, "Il faut au moins 4 équipes pour générer la phase de groupes");

        List<Match> generatedMatches = MatchGeneratorUtil.generateGroupStage(tournament, teams);
        MatchSchedulerUtil.schedule(generatedMatches, tournament.getStartDate(), fieldCount(teams.size()));

        matchRepository.saveAll(generatedMatches);
        initializeStandings(tournament, generatedMatches);
        tournament.setStatus(TournamentStatus.PROGRAMME);
        tournamentRepository.save(tournament);

        return generatedMatches.stream().map(matchMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MatchResponse> generateEliminationPhase(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId));
        if (tournament.getType() != TournamentType.POULES_ELIMINATION) {
            throw new InvalidOperationException("La phase finale n'existe que pour le format poules → élimination");
        }
        if (tournament.getStatus() != TournamentStatus.INSCRIPTION
                && tournament.getStatus() != TournamentStatus.PROGRAMME
                && tournament.getStatus() != TournamentStatus.EN_COURS) {
            throw new InvalidOperationException(
                "Génération impossible : le tournoi est au statut " + tournament.getStatus());
        }

        List<Match> existing = matchRepository.findByTournamentId(tournamentId);
        List<Match> groupMatches = existing.stream()
                .filter(m -> m.getPhase() == MatchPhase.GROUP_STAGE)
                .toList();
        if (groupMatches.isEmpty()) {
            throw new InvalidOperationException("Générez d'abord la phase de groupes");
        }
        if (groupMatches.stream().anyMatch(m -> m.getStatus() != MatchStatus.COMPLETED)) {
            throw new InvalidOperationException(
                "La phase finale est disponible une fois tous les matchs de groupes terminés");
        }
        if (existing.stream().anyMatch(m -> m.getPhase() != MatchPhase.GROUP_STAGE)) {
            throw new InvalidOperationException("La phase finale a déjà été générée");
        }

        // Les deux premiers de chaque poule, tête de série alternée pour ne jamais
        // affronter un coéquipier de poule au premier tour.
        List<String> letters = groupMatches.stream()
                .map(Match::getGroup).filter(Objects::nonNull).distinct().sorted().toList();
        List<Team> qualified = new ArrayList<>();
        List<Team> winners = new ArrayList<>();
        List<Team> runners = new ArrayList<>();
        for (String letter : letters) {
            List<Match> oneGroup = groupMatches.stream()
                    .filter(m -> letter.equals(m.getGroup()))
                    .toList();
            List<Team> groupRanking = rankingGroup(oneGroup);
            if (groupRanking.size() < 2) {
                throw new BusinessException("Poule " + letter + " : classement indisponible");
            }
            winners.add(groupRanking.get(0));
            runners.add(groupRanking.get(1));
        }
        for (int i = 0; i < winners.size(); i++) {
            qualified.add(winners.get(i));
            qualified.add(runners.get((i + 1) % runners.size()));
        }

        List<Match> generatedMatches = MatchGeneratorUtil.generateTwoLeggedKnockout(tournament, qualified);
        MatchSchedulerUtil.schedule(generatedMatches, tournament.getStartDate(), fieldCount(tournament.getTeamCount()));

        matchRepository.saveAll(generatedMatches);
        if (tournament.getStatus() == TournamentStatus.INSCRIPTION) {
            tournament.setStatus(TournamentStatus.PROGRAMME);
            tournamentRepository.save(tournament);
        }
        return generatedMatches.stream().map(matchMapper::toResponse).collect(Collectors.toList());
    }

    /** Classement d'une poule : points, différence puis buts marqués. */
    private List<Team> rankingGroup(List<Match> groupMatches) {
        Map<Long, int[]> stats = new LinkedHashMap<>(); // pts, diff, bp
        Map<Long, Team> teams = new LinkedHashMap<>();
        for (Match m : groupMatches) {
            teams.put(m.getHomeTeam().getId(), m.getHomeTeam());
            teams.put(m.getAwayTeam().getId(), m.getAwayTeam());
            stats.computeIfAbsent(m.getHomeTeam().getId(), k -> new int[3]);
            stats.computeIfAbsent(m.getAwayTeam().getId(), k -> new int[3]);
        }
        for (Match m : groupMatches) {
            if (m.getStatus() != MatchStatus.COMPLETED) continue;
            int h = m.getHomeScore() != null ? m.getHomeScore() : 0;
            int a = m.getAwayScore() != null ? m.getAwayScore() : 0;
            int[] hs = stats.get(m.getHomeTeam().getId());
            int[] as = stats.get(m.getAwayTeam().getId());
            hs[1] += h - a; hs[2] += h;
            as[1] += a - h; as[2] += a;
            if (h > a) { hs[0] += 3; } else if (h < a) { as[0] += 3; } else { hs[0]++; as[0]++; }
        }
        return stats.keySet().stream()
                .sorted((x, y) -> {
                    int[] a = stats.get(x), b = stats.get(y);
                    int c = Integer.compare(b[0], a[0]);
                    if (c != 0) return c;
                    c = Integer.compare(b[1], a[1]);
                    return c != 0 ? c : Integer.compare(b[2], a[2]);
                })
                .map(teams::get).toList();
    }

    private Tournament loadTournamentForGeneration(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId));

        if (tournament.getStatus() != TournamentStatus.INSCRIPTION
                && tournament.getStatus() != TournamentStatus.PROGRAMME) {
            throw new InvalidOperationException(
                "Génération impossible : le tournoi est au statut " + tournament.getStatus());
        }

        if (!matchRepository.findByTournamentId(tournamentId).isEmpty()) {
            throw new InvalidOperationException("Des matchs existent déjà pour ce tournoi");
        }

        if (tournament.getTeamCount() > tournament.getMaxTeams()) {
            throw new BusinessException("Le nombre d'équipes inscrites dépasse la capacité du tournoi");
        }
        return tournament;
    }

    private void ensureMinimumTeams(List<Team> teams, int minimum, String message) {
        if (teams == null || teams.size() < minimum) {
            throw new BusinessException(message);
        }
    }

    private int fieldCount(int teamCount) {
        return Math.max(1, teamCount / 4);
    }

    private void initializeStandings(Tournament tournament) {
        initializeStandings(tournament, List.of());
    }

    /** Crée les classements, en renseignant la poule de chaque équipe (phase de groupes). */
    private void initializeStandings(Tournament tournament, List<Match> groupMatches) {
        Map<Long, String> groupByTeam = new LinkedHashMap<>();
        for (Match m : groupMatches) {
            if (m.getGroup() == null) continue;
            if (m.getHomeTeam() != null) groupByTeam.put(m.getHomeTeam().getId(), m.getGroup());
            if (m.getAwayTeam() != null) groupByTeam.put(m.getAwayTeam().getId(), m.getGroup());
        }
        for (Team team : tournament.getTeams()) {
            if (!standingRepository.existsByTournamentIdAndTeamId(tournament.getId(), team.getId())) {
                Standing standing = new Standing(tournament, team);
                standing.setGroup(groupByTeam.get(team.getId()));
                standingRepository.save(standing);
            } else {
                Standing standing = standingRepository
                        .findByTournamentIdAndTeamId(tournament.getId(), team.getId()).orElse(null);
                if (standing != null && standing.getGroup() == null) {
                    standing.setGroup(groupByTeam.get(team.getId()));
                    standingRepository.save(standing);
                }
            }
        }
    }
}
