package com.example.tournament.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Pages publiques (login, ressources statiques)
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/error").permitAll()

                // API REST : gestion des utilisateurs réservée à l'ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // API REST : consultation pour tous les utilisateurs authentifiés
                .requestMatchers("/api/tournaments/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "USER")
                .requestMatchers("/api/teams/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "USER")
                .requestMatchers("/api/matches/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "USER")
                .requestMatchers("/api/ranking/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "USER")

                // Back-office : gestion des utilisateurs et configuration système (ADMIN uniquement)
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Interface web
                .requestMatchers("/tournaments/new").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/tournaments/*/edit").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/tournaments/*/delete").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/tournaments/*/teams").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/tournaments/*/generate-matches").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/tournaments/*/generate-elimination").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/tournaments/*/qualify").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/teams/new").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/teams/*/edit").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/teams/*/delete").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/teams/*/players").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/matches/*/result").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/matches/*/schedule").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/tournaments/*/status").hasRole("MANAGER")
                .requestMatchers("/tournaments/*/teams/*/remove").hasRole("MANAGER")
                .requestMatchers("/teams/*/players/*/remove").hasRole("MANAGER")
                .requestMatchers("/matches/*/cancel").hasRole("MANAGER")
                .requestMatchers("/") .authenticated()
                .requestMatchers("/dashboard").authenticated()
                .requestMatchers("/tournaments/**").authenticated()
                .requestMatchers("/teams/**").authenticated()
                .requestMatchers("/matches/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .httpBasic(httpBasic -> httpBasic.realmName("tournament-api"))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
