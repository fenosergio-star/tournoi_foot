package com.example.tournament.service.impl;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.CreatePlayerRequest;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.PlayerResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.entity.Country;
import com.example.tournament.entity.Player;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.TeamStatus;
import com.example.tournament.entity.Tournament;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.service.TeamService;
import com.example.tournament.service.TournamentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TeamMapper teamMapper;
    private final TournamentService tournamentService;

    public TeamServiceImpl(TeamRepository teamRepository,
                           PlayerRepository playerRepository,
                           TeamMapper teamMapper,
                           TournamentService tournamentService) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.teamMapper = teamMapper;
        this.tournamentService = tournamentService;
    }

    @Override
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Une équipe avec ce nom existe déjà : " + request.getName());
        }
        Team team = teamMapper.toEntity(request);
        Team saved = teamRepository.save(team);
        tournamentService.autoEnrollTeam(saved.getId());
        return teamMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable avec l'ID : " + id));
        return teamMapper.toResponse(team);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamResponse> getAllTeams(Pageable pageable) {
        return teamRepository.findAll(pageable).map(teamMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamResponse> searchTeams(String name, Country country, TeamStatus status, Pageable pageable) {
        Specification<Team> spec = (root, query, cb) -> cb.conjunction();
        if (name != null && !name.isBlank()) {
            String pattern = "%" + name.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }
        if (country != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("country"), country));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return teamRepository.findAll(spec, pageable).map(teamMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(teamMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeamResponse updateTeam(Long id, UpdateTeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable avec l'ID : " + id));

        if (request.getName() != null) team.setName(request.getName());
        if (request.getLogo() != null) team.setLogo(request.getLogo());
        if (request.getCity() != null) team.setCity(request.getCity());
        if (request.getCountry() != null) team.setCountry(request.getCountry());
        if (request.getCoach() != null) team.setCoach(request.getCoach());

        Team saved = teamRepository.save(team);
        tournamentService.autoEnrollTeam(saved.getId());
        return teamMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Impossible de supprimer : Équipe introuvable avec l'ID : " + id));

        // Retire l'équipe de tous ses tournois (classements inclus).
        // removeTeam refuse si le tournoi est en cours/terminé ou si des matchs existent.
        for (Tournament tournament : List.copyOf(team.getTournaments())) {
            tournamentService.removeTeam(tournament.getId(), id);
        }

        teamRepository.delete(team);
    }

    @Override
    @Transactional
    public PlayerResponse addPlayer(Long teamId, CreatePlayerRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable avec l'ID : " + teamId));

        Player player = new Player();
        player.setFirstName(request.getFirstName());
        player.setLastName(request.getLastName());
        player.setPosition(request.getPosition());
        player.setNumber(request.getNumber());

        team.addPlayer(player);
        teamRepository.save(team);

        return teamMapper.toPlayerResponse(player);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getPlayersByTeam(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Équipe introuvable avec l'ID : " + teamId);
        }
        return playerRepository.findByTeamIdOrderByNumberAsc(teamId)
                .stream()
                .map(teamMapper::toPlayerResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removePlayer(Long teamId, Long playerId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable avec l'ID : " + teamId));

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Joueur introuvable avec l'ID : " + playerId));

        team.removePlayer(player);
        teamRepository.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getAvailableTeams(Long tournamentId) {
        return teamRepository.findAvailableTeamsForTournament(tournamentId)
                .stream()
                .map(teamMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsOfTournament(Long tournamentId) {
        return teamRepository.findAll().stream()
                .filter(t -> t.getTournaments() != null
                        && t.getTournaments().stream().anyMatch(tour -> tour.getId().equals(tournamentId)))
                .map(teamMapper::toResponse)
                .collect(Collectors.toList());
    }
}
