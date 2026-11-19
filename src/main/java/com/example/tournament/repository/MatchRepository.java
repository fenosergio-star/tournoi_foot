package com.example.tournament.repository;

import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

    List<Match> findByTournamentId(Long tournamentId);

    List<Match> findByTournamentIdAndPhase(Long tournamentId, MatchPhase phase);

    List<Match> findByTournamentIdAndStatus(Long tournamentId, MatchStatus status);

    Page<Match> findByTournamentId(Long tournamentId, Pageable pageable);

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tournamentId " +
           "AND (m.homeTeam = :team OR m.awayTeam = :team) " +
           "AND m.matchDate = :date AND m.status != 'CANCELLED'")
    List<Match> findConflictingMatches(@Param("tournamentId") Long tournamentId,
                                        @Param("team") Team team,
                                        @Param("date") LocalDate date);

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tournamentId " +
           "AND ((m.homeTeam.id = :homeTeamId AND m.awayTeam.id = :awayTeamId) " +
           "OR (m.homeTeam.id = :awayTeamId AND m.awayTeam.id = :homeTeamId))")
    List<Match> findDuplicateMatches(@Param("tournamentId") Long tournamentId,
                                      @Param("homeTeamId") Long homeTeamId,
                                      @Param("awayTeamId") Long awayTeamId);

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tournamentId " +
           "AND (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId) " +
           "ORDER BY m.phase ASC, m.round ASC")
    List<Match> findByTournamentAndTeam(@Param("tournamentId") Long tournamentId,
                                         @Param("teamId") Long teamId);

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tournamentId " +
           "AND m.generated = true ORDER BY m.phase ASC, m.round ASC")
    List<Match> findGeneratedMatches(@Param("tournamentId") Long tournamentId);

    @Query("SELECT COUNT(m) FROM Match m WHERE m.tournament.id = :tournamentId " +
           "AND m.homeTeam.id = :teamId AND m.matchDate = :date AND m.status != 'CANCELLED'")
    long countTeamMatchesOnDate(@Param("tournamentId") Long tournamentId,
                                 @Param("teamId") Long teamId,
                                 @Param("date") LocalDate date);
}
