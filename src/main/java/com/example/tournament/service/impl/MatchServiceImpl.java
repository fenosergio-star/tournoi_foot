package com.example.tournament.service.impl;

import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.request.UpdateMatchResultRequest;
import com.example.tournament.dto.response.BracketResponse;
import com.example.tournament.dto.response.BracketRoundResponse;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.*;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.MatchService;
import com.example.tournament.service.RankingService;
import com.example.tournament.util.MatchSchedulerUtil;
import com.example.tournament.util.PhaseLabels;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final RankingService rankingService;
    private final TournamentRepository tournamentRepository;

    public MatchServiceImpl(MatchRepository matchRepository,
                            MatchMapper matchMapper,
                            RankingService rankingService,
                            TournamentRepository tournamentRepository) {
        this.matchRepository = matchRepository;
        this.matchMapper = matchMapper;
        this.rankingService = rankingService;
        this.tournamentRepository = tournamentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentType getTournamentType(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId))
                .getType();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match introuvable avec l'ID : " + id));
        return matchMapper.toResponse(match);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesByTournament(Long tournamentId) {
        return sortMatches(matchRepository.findByTournamentId(tournamentId))
                .stream()
                .map(matchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatchesByTournament(Long tournamentId, Pageable pageable) {
        return matchRepository.findByTournamentId(tournamentId, pageable).map(matchMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> searchMatchesByTournament(Long tournamentId, MatchStatus status, MatchPhase phase,
                                                         LocalDate date, Pageable pageable) {
        Specification<Match> spec = (root, query, cb) ->
                cb.equal(root.get("tournament").get("id"), tournamentId);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (phase != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("phase"), phase));
        }
        if (date != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("matchDate"), date));
        }
        return matchRepository.findAll(spec, pageable).map(matchMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesByTournamentAndPhase(Long tournamentId, String phase) {
        MatchPhase matchPhase;
        try {
            matchPhase = MatchPhase.valueOf(phase.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Phase inconnue : " + phase);
        }
        return sortMatches(matchRepository.findByTournamentIdAndPhase(tournamentId, matchPhase))
                .stream()
                .map(matchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MatchResponse updateMatchResult(Long matchId, UpdateMatchResultRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match introuvable avec l'ID : " + matchId));

        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new InvalidOperationException("Impossible d'enregistrer le résultat d'un match annulé");
        }
        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new InvalidOperationException(
                "Impossible d'enregistrer un résultat : les équipes du match ne sont pas encore connues");
        }

        Integer homePenalties = request.getHomePenalties();
        Integer awayPenalties = request.getAwayPenalties();

        boolean draw = request.getHomeScore().equals(request.getAwayScore());
        if (match.getLeg() != null) {
            // Confrontation aller-retour : nul possible à chaque manche, les tirs
            // au but ne sont exigés qu'au retour si le score cumulé reste égal.
            if (match.getLeg() == 1) {
                homePenalties = null;
                awayPenalties = null;
            } else {
                Match leg1 = findOtherLeg(match);
                if (leg1 == null || leg1.getStatus() != MatchStatus.COMPLETED) {
                    throw new InvalidOperationException(
                        "Le match aller doit être terminé avant de saisir le retour");
                }
                if (aggregateLevel(leg1, match, request)) {
                    if (homePenalties == null || awayPenalties == null) {
                        throw new BusinessException(
                            "Score cumulé et buts extérieurs égaux : saisissez les tirs au but du match retour");
                    }
                    if (homePenalties.equals(awayPenalties)) {
                        throw new BusinessException("Les tirs au but ne peuvent pas être à égalité");
                    }
                } else {
                    homePenalties = null;
                    awayPenalties = null;
                }
            }
        } else if (match.isKnockout()) {
            if (draw) {
                if (homePenalties == null || awayPenalties == null) {
                    throw new BusinessException(
                        "Un match à élimination directe ne peut pas se terminer par un match nul : "
                        + "saisissez les tirs au but");
                }
                if (homePenalties.equals(awayPenalties)) {
                    throw new BusinessException("Les tirs au but ne peuvent pas être à égalité");
                }
            } else {
                // Score décisif : on ignore/efface d'éventuels tirs au but
                homePenalties = null;
                awayPenalties = null;
            }
        } else {
            homePenalties = null;
            awayPenalties = null;
        }

        match.setHomeScore(request.getHomeScore());
        match.setAwayScore(request.getAwayScore());
        match.setHomePenalties(homePenalties);
        match.setAwayPenalties(awayPenalties);
        match.setStatus(MatchStatus.COMPLETED);

        Match saved = matchRepository.save(match);

        // Recalcul complet du classement (jamais de modification sans recalcul)
        rankingService.recalculateRanking(saved.getTournament().getId());

        if (saved.isKnockout()) {
            propagateKnockoutResult(saved);
        }
        updateTournamentCompletion(saved.getTournament());

        return matchMapper.toResponse(saved);
    }

    /** Autre manche de la même confrontation aller-retour. */
    private Match findOtherLeg(Match match) {
        return matchRepository.findByTournamentId(match.getTournament().getId()).stream()
                .filter(m -> !m.getId().equals(match.getId()))
                .filter(m -> m.getRound() != null && m.getRound().equals(match.getRound()))
                .filter(m -> m.getBracketPosition() != null && m.getBracketPosition().equals(match.getBracketPosition()))
                .filter(m -> m.getPhase() == match.getPhase())
                .filter(m -> m.getLeg() != null && !m.getLeg().equals(match.getLeg()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Le score cumulé et les buts extérieurs sont-ils toujours égaux après le
     * retour (ce qui impose les tirs au but) ? Calculé sur le score soumis.
     */
    private boolean aggregateLevel(Match leg1, Match leg2, UpdateMatchResultRequest request) {
        Team home1 = leg1.getHomeTeam(); // tête de série (match aller)
        Team away1 = leg1.getAwayTeam();
        if (leg2.getHomeTeam() == null || leg2.getAwayTeam() == null) {
            return true;
        }
        boolean inverted = leg2.getHomeTeam().equals(away1);
        int goals1 = safe(leg1.getHomeScore());
        int goals2 = inverted ? safe(request.getAwayScore()) : safe(request.getHomeScore());
        int oppGoals1 = safe(leg1.getAwayScore());
        int oppGoals2 = inverted ? safe(request.getHomeScore()) : safe(request.getAwayScore());
        int aggFor = goals1 + goals2;
        int aggAgainst = oppGoals1 + oppGoals2;
        if (aggFor != aggAgainst) return false;
        // Buts extérieurs : équipes domiciles inversées entre les deux manches.
        int awayFor = inverted ? safe(request.getAwayScore()) : safe(leg1.getAwayScore());
        int awayAgainst = inverted ? safe(leg1.getHomeScore()) : safe(request.getHomeScore());
        return awayFor == awayAgainst;
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Vainqueur d'une confrontation aller-retour : score cumulé, puis buts
     * extérieurs, puis tirs au but du match retour.
     */
    private Team resolveTieWinner(Match leg1, Match leg2) {
        Team home1 = leg1.getHomeTeam();
        Team away1 = leg1.getAwayTeam();
        if (home1 == null || away1 == null || leg2.getHomeTeam() == null || leg2.getAwayTeam() == null) {
            return null;
        }
        boolean inverted = leg2.getHomeTeam().equals(away1);
        int homeFor = safe(leg1.getHomeScore()) + (inverted ? safe(leg2.getAwayScore()) : safe(leg2.getHomeScore()));
        int homeAgainst = safe(leg1.getAwayScore()) + (inverted ? safe(leg2.getHomeScore()) : safe(leg2.getAwayScore()));
        if (homeFor > homeAgainst) return home1;
        if (homeFor < homeAgainst) return away1;
        int homeAway = inverted ? safe(leg2.getAwayScore()) : safe(leg1.getAwayScore());
        int awayAway = inverted ? safe(leg1.getHomeScore()) : safe(leg2.getHomeScore());
        if (homeAway > awayAway) return home1;
        if (homeAway < awayAway) return away1;
        // Tirs au but du retour (paires inversées si besoin)
        Integer homePens = inverted ? leg2.getAwayPenalties() : leg2.getHomePenalties();
        Integer awayPens = inverted ? leg2.getHomePenalties() : leg2.getAwayPenalties();
        if (homePens == null || awayPens == null || homePens.equals(awayPens)) return null;
        return homePens > awayPens ? home1 : away1;
    }

    /**
     * Fait progresser le vainqueur vers le tour suivant. En aller-retour, le
     * vainqueur n'est désigné qu'une fois les deux manches terminées ; il est
     * alors attribué aux deux matchs du tour suivant (domicile à l'aller,
     * extérieur au retour). Pour le premier tour d'un arbre classique, le
     * perdant des demi-finales rejoint la petite finale.
     */
    private void propagateKnockoutResult(Match match) {
        if (match.getPhase() == MatchPhase.FINAL || match.getPhase() == MatchPhase.THIRD_PLACE) {
            return;
        }

        Team winner;
        Team loser = null;

        if (match.getLeg() != null) {
            Match leg1 = match.getLeg() == 1 ? match : findOtherLeg(match);
            Match leg2 = match.getLeg() == 1 ? findOtherLeg(match) : match;
            if (leg1 == null || leg2 == null
                    || leg1.getStatus() != MatchStatus.COMPLETED
                    || leg2.getStatus() != MatchStatus.COMPLETED) {
                return; // confrontation pas encore décidée
            }
            winner = resolveTieWinner(leg1, leg2);
            loser = winner != null ? (winner.equals(leg1.getHomeTeam()) ? leg1.getAwayTeam() : leg1.getHomeTeam()) : null;
            if (winner == null) return;
        } else {
            winner = match.getWinner();
            if (winner == null) return;
            loser = match.getLoser();
        }

        Tournament tournament = match.getTournament();
        int nextRound = match.getRound() + 1;
        int nextPosition = match.getBracketPosition() / 2;
        boolean homeSlot = match.getBracketPosition() % 2 == 0;

        List<Match> nextTie = findTie(tournament, nextRound, nextPosition);
        if (!nextTie.isEmpty()) {
            for (Match next : nextTie) {
                assignTeam(next, nextTie.size() > 1 ? (next.getLeg() == null || next.getLeg() == 1 ? homeSlot : !homeSlot) : homeSlot, winner);
            }
        }

        if (match.getPhase() == MatchPhase.SEMI_FINAL && loser != null && match.getLeg() == null) {
            Match thirdPlace = findThirdPlace(tournament);
            if (thirdPlace != null) {
                assignTeam(thirdPlace, homeSlot, loser);
            }
        }
    }

    private void assignTeam(Match target, boolean homeSide, Team team) {
        if (target.getStatus() == MatchStatus.COMPLETED) {
            resetTieAndDescendants(target);
        }
        if (homeSide) {
            target.setHomeTeam(team);
        } else {
            target.setAwayTeam(team);
        }
        matchRepository.save(target);
    }

    /** Remet à zéro les deux manches d'une confrontation et leurs descendants. */
    private void resetTieAndDescendants(Match match) {
        List<Match> tie = match.getLeg() != null
                ? findTie(match.getTournament(), match.getRound(), match.getBracketPosition())
                : List.of(match);
        for (Match m : tie) {
            resetMatchAndDescendants(m);
        }
    }

    private void resetMatchAndDescendants(Match match) {
        match.setStatus(MatchStatus.PLANNED);
        match.setHomeScore(null);
        match.setAwayScore(null);
        match.setHomePenalties(null);
        match.setAwayPenalties(null);
        matchRepository.save(match);

        if (match.getPhase() == MatchPhase.FINAL || match.getPhase() == MatchPhase.THIRD_PLACE) {
            return;
        }
        boolean homeSlot = match.getBracketPosition() % 2 == 0;

        int nextRound = match.getRound() + 1;
        int nextPosition = match.getBracketPosition() / 2;
        List<Match> nextTie = findTie(match.getTournament(), nextRound, nextPosition);
        boolean first = true;
        for (Match next : nextTie) {
            boolean nextHome = nextTie.size() > 1 && next.getLeg() != null && next.getLeg() == 2 ? !homeSlot : homeSlot;
            if (nextHome) {
                next.setHomeTeam(null);
            } else {
                next.setAwayTeam(null);
            }
            matchRepository.save(next);
            if (first) {
                resetMatchAndDescendants(next);
                first = false;
            }
        }

        if (match.getPhase() == MatchPhase.SEMI_FINAL && match.getLeg() == null) {
            Match thirdPlace = findThirdPlace(match.getTournament());
            if (thirdPlace != null) {
                if (homeSlot) {
                    thirdPlace.setHomeTeam(null);
                } else {
                    thirdPlace.setAwayTeam(null);
                }
                resetMatchAndDescendants(thirdPlace);
            }
        }
    }

    /** Les manches (aller/retour) d'un tour et d'une position donnés. */
    private List<Match> findTie(Tournament tournament, int round, int position) {
        return matchRepository.findByTournamentId(tournament.getId()).stream()
                .filter(m -> m.getRound() != null && m.getRound() == round)
                .filter(m -> m.getBracketPosition() != null && m.getBracketPosition() == position)
                .filter(m -> m.getPhase() != MatchPhase.THIRD_PLACE)
                .filter(m -> m.getPhase() != MatchPhase.GROUP_STAGE)
                .toList();
    }

    private Match findMatch(Tournament tournament, int round, int position) {
        return findTie(tournament, round, position).stream().findFirst().orElse(null);
    }

    private Match findThirdPlace(Tournament tournament) {
        return matchRepository.findByTournamentId(tournament.getId()).stream()
                .filter(m -> m.getPhase() == MatchPhase.THIRD_PLACE)
                .findFirst()
                .orElse(null);
    }

    private void updateTournamentCompletion(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.ANNULE) {
            return;
        }
        List<Match> matches = matchRepository.findByTournamentId(tournament.getId());
        boolean hasPlayable = matches.stream().anyMatch(m -> m.getPhase() != MatchPhase.THIRD_PLACE);
        boolean allCompleted = matches.stream()
                .filter(m -> m.getPhase() != MatchPhase.THIRD_PLACE)
                .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED);

        // En format poules, les matchs de groupes terminés ne suffisent pas :
        // la phase finale doit être générée puis jouée.
        if (tournament.getType() == TournamentType.POULES_ELIMINATION) {
            boolean hasElimination = matches.stream()
                    .anyMatch(m -> m.getPhase() != MatchPhase.GROUP_STAGE && m.getPhase() != MatchPhase.THIRD_PLACE);
            if (!hasElimination) {
                return;
            }
        }

        if (hasPlayable && allCompleted) {
            if (tournament.getStatus() != TournamentStatus.TERMINE) {
                tournament.setStatus(TournamentStatus.TERMINE);
                tournamentRepository.save(tournament);
            }
        } else if (tournament.getStatus() == TournamentStatus.TERMINE) {
            // Un résultat en aval a été réinitialisé : le tournoi reprend
            tournament.setStatus(TournamentStatus.EN_COURS);
            tournamentRepository.save(tournament);
        }
    }

    @Override
    @Transactional
    public MatchResponse rescheduleMatch(Long matchId, RescheduleMatchRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match introuvable avec l'ID : " + matchId));

        Tournament tournament = match.getTournament();
        if (tournament.getStatus() == TournamentStatus.TERMINE
                || tournament.getStatus() == TournamentStatus.ANNULE) {
            throw new InvalidOperationException(
                "Impossible de reporter un match d'un tournoi " + tournament.getStatus());
        }

        String field = request.getField() != null ? request.getField() : match.getField();
        List<Match> existing = matchRepository.findByTournamentId(tournament.getId());

        if (MatchSchedulerUtil.hasConflict(match.getHomeTeam(), match.getAwayTeam(),
                request.getMatchDate(), request.getMatchTime(), field, existing, matchId)) {
            throw new ConflictException(
                "Créneau indisponible : une équipe joue déjà ou le terrain est occupé à cette date/heure");
        }

        match.setMatchDate(request.getMatchDate());
        match.setMatchTime(request.getMatchTime());
        if (field != null) {
            match.setField(field);
        }
        return matchMapper.toResponse(matchRepository.save(match));
    }

    @Override
    @Transactional
    public void cancelMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match introuvable avec l'ID : " + matchId));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new InvalidOperationException("Impossible d'annuler un match déjà terminé");
        }

        match.setStatus(MatchStatus.CANCELLED);
        matchRepository.save(match);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesByTeam(Long tournamentId, Long teamId) {
        return matchRepository.findByTournamentAndTeam(tournamentId, teamId)
                .stream()
                .map(matchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchResponse> getUpcomingMatches(Long tournamentId) {
        return matchRepository.findByTournamentIdAndStatus(tournamentId, MatchStatus.PLANNED)
                .stream()
                .map(matchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BracketResponse getBracket(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId));

        BracketResponse bracket = new BracketResponse(tournamentId, tournament.getName());

        List<Match> knockoutMatches = sortMatches(matchRepository.findByTournamentId(tournamentId))
                .stream()
                .filter(Match::isKnockout)
                .collect(Collectors.toList());

        Map<Integer, List<Match>> byRound = knockoutMatches.stream()
                .filter(m -> m.getRound() != null && m.getPhase() != MatchPhase.THIRD_PLACE)
                .collect(Collectors.groupingBy(Match::getRound, java.util.TreeMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<Match>> entry : byRound.entrySet()) {
            List<Match> roundMatches = entry.getValue();
            MatchPhase phase = roundMatches.get(0).getPhase();
            BracketRoundResponse round = new BracketRoundResponse(
                    phase, PhaseLabels.label(phase), entry.getKey());
            round.setMatches(roundMatches.stream().map(matchMapper::toResponse).collect(Collectors.toList()));
            bracket.getRounds().add(round);
        }

        Match thirdPlace = findThirdPlace(tournament);
        if (thirdPlace != null) {
            BracketRoundResponse round = new BracketRoundResponse(
                    MatchPhase.THIRD_PLACE, PhaseLabels.label(MatchPhase.THIRD_PLACE), thirdPlace.getRound());
            round.getMatches().add(matchMapper.toResponse(thirdPlace));
            bracket.getRounds().add(round);
        }

        return bracket;
    }

    private List<Match> sortMatches(List<Match> matches) {
        return matches.stream()
                .sorted(Comparator
                        .comparing((Match m) -> m.getRound() == null ? Integer.MAX_VALUE : m.getRound())
                        .thenComparing(m -> m.getBracketPosition() == null ? Integer.MAX_VALUE : m.getBracketPosition())
                        .thenComparing(m -> m.getMatchDate() == null ? java.time.LocalDate.MAX : m.getMatchDate())
                        .thenComparing(m -> m.getMatchTime() == null ? java.time.LocalTime.MAX : m.getMatchTime()))
                .collect(Collectors.toList());
    }
}
