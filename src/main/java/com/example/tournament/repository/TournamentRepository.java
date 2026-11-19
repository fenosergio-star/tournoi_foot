package com.example.tournament.repository;

import com.example.tournament.entity.Country;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {

    // Méthode dérivée (Spring génère la requête SQL automatiquement)
    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findBySportIgnoreCase(String sport);

    List<Tournament> findByCountry(Country country);

    Optional<Tournament> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Page<Tournament> findByStatus(TournamentStatus status, Pageable pageable);

    Page<Tournament> findBySportIgnoreCase(String sport, Pageable pageable);

    Page<Tournament> findByStatusAndSportIgnoreCase(TournamentStatus status, String sport, Pageable pageable);

    Page<Tournament> findByNameContainingIgnoreCase(String name, Pageable pageable);
}