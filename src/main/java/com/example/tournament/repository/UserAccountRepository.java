package com.example.tournament.repository;

import com.example.tournament.entity.Role;
import com.example.tournament.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRole(Role role);
}
