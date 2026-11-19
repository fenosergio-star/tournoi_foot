package com.example.tournament.entity;

/**
 * Rôles applicatifs, du plus privilégié au moins privilégié.
 * <p>
 * Hiérarchie (cumulative) : ADMIN &gt; MANAGER &gt; OPERATOR &gt; USER.
 * <ul>
 *   <li>ADMIN : configuration globale du système et gestion des utilisateurs.</li>
 *   <li>MANAGER : création et configuration des tournois, gestion des formats.</li>
 *   <li>OPERATOR : inscription des équipes, génération des matchs, saisie des résultats.</li>
 *   <li>USER : consultation des classements, calendriers et arbres.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    MANAGER,
    OPERATOR,
    USER
}
