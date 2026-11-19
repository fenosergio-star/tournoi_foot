package com.example.tournament.repository;

import com.example.tournament.entity.Country;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.TeamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    List<Team> findByStatus(TeamStatus status);

    List<Team> findByCityIgnoreCase(String city);

    List<Team> findByCountry(Country country);

    Optional<Team> findByNameIgnoreCase(String name);

    Page<Team> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT t FROM Team t WHERE t.status = 'ACTIVE' AND t.id NOT IN " +
           "(SELECT tt.id FROM Tournament tour JOIN tour.teams tt WHERE tour.id = :tournamentId)")
    List<Team> findAvailableTeamsForTournament(@Param("tournamentId") Long tournamentId);

    boolean existsByNameIgnoreCase(String name);
}
