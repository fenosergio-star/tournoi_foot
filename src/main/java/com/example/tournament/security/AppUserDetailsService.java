package com.example.tournament.security;

import com.example.tournament.entity.UserAccount;
import com.example.tournament.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Charge les utilisateurs depuis la base de données.
 * Les rôles sont exposés sous la forme {@code ROLE_ADMIN}, {@code ROLE_MANAGER}, etc.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userRepository;

    public AppUserDetailsService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .roles(user.getRole().name())
                .build();
    }
}
