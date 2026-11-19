package com.example.tournament.repository;

import com.example.tournament.entity.Standing;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandingRepository extends JpaRepository<Standing, Long> {

    List<Standing> findByTournamentIdOrderByPositionAsc(Long tournamentId);

    Optional<Standing> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    @Query("SELECT s FROM Standing s WHERE s.tournament.id = :tournamentId " +
           "ORDER BY s.points DESC, (s.goalsFor - s.goalsAgainst) DESC, s.goalsFor DESC")
    List<Standing> findRankingByTournament(@Param("tournamentId") Long tournamentId);

    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);
}
