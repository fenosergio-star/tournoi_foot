package com.example.tournament.service.impl;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.*;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TournamentMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.StandingRepository;
import com.example.tournament.service.RankingService;
import com.example.tournament.service.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TournamentServiceImpl implements TournamentService {

    private static final Logger logger = LoggerFactory.getLogger(TournamentServiceImpl.class);

    private static final String CHAMPIONS_LEAGUE_NAME = "Ligue des Champions";

    private final TournamentRepository tournamentRepository;
    private final TournamentMapper tournamentMapper;
    private final TeamRepository teamRepository;
    private final StandingRepository standingRepository;
    private final MatchRepository matchRepository;
    private final RankingService rankingService;

    public TournamentServiceImpl(TournamentRepository tournamentRepository,
                                  TournamentMapper tournamentMapper,
                                  TeamRepository teamRepository,
                                  StandingRepository standingRepository,
                                  MatchRepository matchRepository,
                                  RankingService rankingService) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentMapper = tournamentMapper;
        this.teamRepository = teamRepository;
        this.standingRepository = standingRepository;
        this.matchRepository = matchRepository;
        this.rankingService = rankingService;
    }

    @Override
    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("La date de fin ne peut pas être antérieure à la date de début");
        }
        Tournament tournament = tournamentMapper.toEntity(request);
        Tournament saved = tournamentRepository.save(tournament);
        if (saved.getCountry() != null) {
            enrollCountryTeams(saved);
        }
        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentResponse getTournamentById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + id));
        return tournamentMapper.toResponse(tournament);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TournamentResponse> getAllTournaments(Pageable pageable) {
        return tournamentRepository.findAll(pageable).map(tournamentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TournamentResponse> searchTournaments(TournamentStatus status, String sport,
                                                      String name, Pageable pageable) {
        Specification<Tournament> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (sport != null && !sport.isBlank()) {
            String value = sport.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("sport")), value));
        }
        if (name != null && !name.isBlank()) {
            String pattern = "%" + name.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }
        return tournamentRepository.findAll(spec, pageable).map(tournamentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TournamentResponse> getAllTournaments() {
        return tournamentRepository.findAll()
                .stream()
                .map(tournamentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TournamentResponse updateTournament(Long id, UpdateTournamentRequest request) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + id));

        if (tournament.getStatus() == TournamentStatus.TERMINE) {
            throw new InvalidOperationException("Impossible de modifier un tournoi terminé");
        }
        if (tournament.getStatus() == TournamentStatus.EN_COURS) {
            throw new InvalidOperationException("Impossible de modifier un tournoi en cours");
        }
        if (tournament.getStatus() == TournamentStatus.ANNULE) {
            throw new InvalidOperationException("Impossible de modifier un tournoi annulé");
        }

        if (request.getName() != null) tournament.setName(request.getName());
        if (request.getSport() != null) tournament.setSport(request.getSport());
        if (request.getDescription() != null) tournament.setDescription(request.getDescription());
        if (request.getStartDate() != null) tournament.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) tournament.setEndDate(request.getEndDate());
        if (request.getLocation() != null) tournament.setLocation(request.getLocation());
        if (request.getCountry() != null) tournament.setCountry(request.getCountry());
        if (request.getMaxTeams() != null) {
            if (request.getMaxTeams() < tournament.getTeamCount()) {
                throw new BusinessException(
                    "Le nombre maximum d'équipes ne peut pas être inférieur au nombre d'équipes inscrites ("
                    + tournament.getTeamCount() + ")");
            }
            tournament.setMaxTeams(request.getMaxTeams());
        }
        if (request.getType() != null) tournament.setType(request.getType());
        if (request.getPointsWin() != null) tournament.setPointsWin(request.getPointsWin());
        if (request.getPointsDraw() != null) tournament.setPointsDraw(request.getPointsDraw());
        if (request.getPointsLoss() != null) tournament.setPointsLoss(request.getPointsLoss());
        if (request.getTiebreakers() != null && !request.getTiebreakers().isBlank()) {
            tournament.setTiebreakers(request.getTiebreakers());
        }

        Tournament saved = tournamentRepository.save(tournament);
        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impossible de supprimer : Tournoi introuvable avec l'ID : " + id));

        if (tournament.getStatus() == TournamentStatus.EN_COURS) {
            throw new InvalidOperationException("Impossible de supprimer un tournoi en cours");
        }
        if (tournament.getStatus() == TournamentStatus.TERMINE) {
            throw new InvalidOperationException("Impossible de supprimer un tournoi terminé");
        }

        tournamentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public TournamentResponse updateStatus(Long id, TournamentStatus newStatus) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + id));

        validateStatusTransition(tournament.getStatus(), newStatus);

        if (newStatus == TournamentStatus.EN_COURS
                && matchRepository.findByTournamentId(id).isEmpty()) {
            throw new InvalidOperationException(
                "Impossible de démarrer un tournoi sans matchs générés");
        }

        tournament.setStatus(newStatus);

        if (newStatus == TournamentStatus.INSCRIPTION) {
            initializeStandings(tournament);
        }

        Tournament saved = tournamentRepository.save(tournament);
        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TournamentResponse registerTeam(Long tournamentId, Long teamId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId));

        if (tournament.getStatus() != TournamentStatus.BROUILLON
                && tournament.getStatus() != TournamentStatus.INSCRIPTION) {
            throw new InvalidOperationException(
                "Inscription impossible : le tournoi est au statut " + tournament.getStatus());
        }

        if (tournament.getStatus() == TournamentStatus.INSCRIPTION && tournament.getStartDate().isBefore(java.time.LocalDate.now())) {
            throw new InvalidOperationException("Inscription impossible : le tournoi a déjà commencé");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable avec l'ID : " + teamId));

        if (tournament.isTeamRegistered(team)) {
            throw new ConflictException("Cette équipe est déjà inscrite à ce tournoi");
        }

        if (tournament.isFull()) {
            throw new ConflictException("Le tournoi a atteint son nombre maximum d'équipes (" + tournament.getMaxTeams() + ")");
        }

        if (tournament.getStatus() == TournamentStatus.BROUILLON) {
            tournament.setStatus(TournamentStatus.INSCRIPTION);
        }

        tournament.addTeam(team);
        Tournament saved = tournamentRepository.save(tournament);

        if (!standingRepository.existsByTournamentIdAndTeamId(saved.getId(), team.getId())) {
            standingRepository.save(new Standing(saved, team));
        }

        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<String> qualifyTopTeams(Long sourceTournamentId, Long targetTournamentId, int count) {
        if (count < 1) {
            throw new BusinessException("Le nombre de qualifiés doit être au moins 1");
        }

        Tournament source = tournamentRepository.findById(sourceTournamentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Tournoi source introuvable avec l'ID : " + sourceTournamentId));

        Tournament target = targetTournamentId != null
                ? tournamentRepository.findById(targetTournamentId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Tournoi cible introuvable avec l'ID : " + targetTournamentId))
                : tournamentRepository.findByNameIgnoreCase(CHAMPIONS_LEAGUE_NAME)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        CHAMPIONS_LEAGUE_NAME + " introuvable : créez d'abord la compétition cible"));

        if (source.getId().equals(target.getId())) {
            throw new BusinessException("Un tournoi ne peut pas se qualifier lui-même");
        }
        if (source.getStatus() != TournamentStatus.TERMINE) {
            throw new InvalidOperationException(
                "Le tournoi source doit être terminé pour qualifier ses premiers (statut " + source.getStatus() + ")");
        }
        if (target.getStatus() != TournamentStatus.BROUILLON
                && target.getStatus() != TournamentStatus.INSCRIPTION) {
            throw new InvalidOperationException(
                "Le tournoi cible n'accepte plus d'inscriptions (statut " + target.getStatus() + ")");
        }

        List<StandingResponse> qualifiedRanking = rankingService.getRankingByTournament(source.getId()).stream()
                .filter(s -> s.getPosition() != null && s.getPosition() <= count)
                .sorted(Comparator.comparing(StandingResponse::getPosition))
                .collect(Collectors.toList());
        if (qualifiedRanking.size() < count) {
            throw new BusinessException("Classement incomplet : " + qualifiedRanking.size()
                    + " équipe(s) classée(s) sur les " + count + " attendues");
        }

        List<String> qualified = new ArrayList<>();
        boolean changed = false;
        for (StandingResponse standing : qualifiedRanking) {
            Team team = teamRepository.findById(standing.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Équipe introuvable avec l'ID : " + standing.getTeamId()));
            if (target.isTeamRegistered(team)) {
                continue;
            }
            if (target.isFull()) {
                throw new ConflictException(
                    "Le tournoi cible est complet (" + target.getMaxTeams() + " équipes)");
            }
            target.addTeam(team);
            if (!standingRepository.existsByTournamentIdAndTeamId(target.getId(), team.getId())) {
                standingRepository.save(new Standing(target, team));
            }
            qualified.add(team.getName());
            changed = true;
        }

        if (changed) {
            if (target.getStatus() == TournamentStatus.BROUILLON) {
                target.setStatus(TournamentStatus.INSCRIPTION);
            }
            tournamentRepository.save(target);
            logger.info("{} équipe(s) qualifiée(s) de {} vers {} : {}",
                    qualified.size(), source.getName(), target.getName(), qualified);
        }
        return qualified;
    }

    @Override
    @Transactional
    public TournamentResponse removeTeam(Long tournamentId, Long teamId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'ID : " + tournamentId));

        if (tournament.getStatus() == TournamentStatus.EN_COURS) {
            throw new InvalidOperationException("Impossible de retirer une équipe pendant le tournoi");
        }
        if (tournament.getStatus() == TournamentStatus.TERMINE) {
            throw new InvalidOperationException("Impossible de retirer une équipe d'un tournoi terminé");
        }
        if (tournament.getStatus() == TournamentStatus.PROGRAMME
                || !matchRepository.findByTournamentId(tournamentId).isEmpty()) {
            throw new InvalidOperationException(
                "Impossible de retirer une équipe : le calendrier des matchs a déjà été généré");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable avec l'ID : " + teamId));

        if (!tournament.isTeamRegistered(team)) {
            throw new ConflictException("Cette équipe n'est pas inscrite à ce tournoi");
        }

        tournament.removeTeam(team);
        Tournament saved = tournamentRepository.save(tournament);

        standingRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .ifPresent(standingRepository::delete);

        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TournamentResponse> getTournamentsByStatus(TournamentStatus status) {
        return tournamentRepository.findByStatus(status)
                .stream()
                .map(tournamentMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateStatusTransition(TournamentStatus current, TournamentStatus next) {
        boolean valid = switch (current) {
            case BROUILLON -> next == TournamentStatus.INSCRIPTION || next == TournamentStatus.ANNULE;
            case INSCRIPTION -> next == TournamentStatus.PROGRAMME || next == TournamentStatus.ANNULE;
            case PROGRAMME -> next == TournamentStatus.EN_COURS || next == TournamentStatus.ANNULE;
            case EN_COURS -> next == TournamentStatus.TERMINE || next == TournamentStatus.ANNULE;
            case TERMINE -> false;
            case ANNULE -> false;
        };
        if (!valid) {
            throw new InvalidOperationException(
                "Transition de statut invalide : " + current + " → " + next);
        }
    }

    private void initializeStandings(Tournament tournament) {
        for (Team team : tournament.getTeams()) {
            if (!standingRepository.existsByTournamentIdAndTeamId(tournament.getId(), team.getId())) {
                standingRepository.save(new Standing(tournament, team));
            }
        }
    }

    @Override
    @Transactional
    public void autoEnrollTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null || team.getCountry() == null) {
            return;
        }
        for (Tournament tournament : tournamentRepository.findByCountry(team.getCountry())) {
            if (!isOpenForEnrollment(tournament) || tournament.isFull()
                    || tournament.isTeamRegistered(team)) {
                continue;
            }
            registerQuietly(tournament.getId(), team.getId());
        }
    }

    /**
     * Inscription automatique : rattache toutes les équipes du pays au tournoi créé.
     * Les échecs (complet, déjà inscrit, créneau...) sont ignorés silencieusement.
     */
    private void enrollCountryTeams(Tournament tournament) {
        for (Team team : teamRepository.findByCountry(tournament.getCountry())) {
            if (tournament.isFull()) {
                break;
            }
            if (tournament.isTeamRegistered(team)) {
                continue;
            }
            registerQuietly(tournament.getId(), team.getId());
        }
    }

    private void registerQuietly(Long tournamentId, Long teamId) {
        try {
            registerTeam(tournamentId, teamId);
        } catch (RuntimeException ex) {
            logger.debug("Inscription automatique ignorée (tournoi={}, équipe={}) : {}",
                    tournamentId, teamId, ex.getMessage());
        }
    }

    private boolean isOpenForEnrollment(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.BROUILLON) {
            return true;
        }
        if (tournament.getStatus() == TournamentStatus.INSCRIPTION) {
            return tournament.getStartDate() == null
                    || !tournament.getStartDate().isBefore(java.time.LocalDate.now());
        }
        return false;
    }
}
