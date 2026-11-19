package com.example.tournament.util;

import com.example.tournament.entity.Match;
import com.example.tournament.entity.Team;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Planificateur de matchs. Garantit qu'une équipe ne joue pas deux fois le même
 * jour et que deux matchs ne se chevauchent pas sur le même terrain au même
 * créneau horaire.
 */
public final class MatchSchedulerUtil {

    private static final LocalTime[] DEFAULT_TIMES = {
        LocalTime.of(14, 0),
        LocalTime.of(16, 0),
        LocalTime.of(18, 0),
        LocalTime.of(20, 0)
    };

    private MatchSchedulerUtil() {}

    public static void schedule(List<Match> matches, LocalDate startDate, int fieldCount) {
        List<String> fields = new ArrayList<>();
        for (int i = 1; i <= Math.max(1, fieldCount); i++) {
            fields.add("Terrain " + i);
        }
        schedule(matches, startDate, fields, DEFAULT_TIMES);
    }

    public static void schedule(List<Match> matches, LocalDate startDate,
                                List<String> fields, LocalTime[] times) {
        if (matches == null || matches.isEmpty()) return;
        if (fields == null || fields.isEmpty()) {
            fields = List.of("Terrain 1");
        }
        if (times == null || times.length == 0) {
            times = DEFAULT_TIMES;
        }

        Set<String> fieldOccupied = new HashSet<>();
        Map<LocalDate, Set<Long>> teamsBusyOnDay = new HashMap<>();

        int slotCursor = 0;
        int slotsPerDay = times.length * fields.size();
        int maxSlots = matches.size() * slotsPerDay + slotsPerDay;

        for (Match match : matches) {
            if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
                // Match en attente (arbre d'élimination) : planifié plus tard.
                continue;
            }
            boolean assigned = false;
            int attempts = 0;
            while (!assigned && slotCursor <= maxSlots) {
                int dayOffset = slotCursor / slotsPerDay;
                int within = slotCursor % slotsPerDay;
                int timeIndex = within / fields.size();
                int fieldIndex = within % fields.size();

                LocalDate date = startDate.plusDays(dayOffset);
                LocalTime time = times[timeIndex];
                String field = fields.get(fieldIndex);

                String fieldKey = field + "@" + date + "T" + time;
                Set<Long> busy = teamsBusyOnDay.computeIfAbsent(date, d -> new HashSet<>());

                boolean fieldFree = !fieldOccupied.contains(fieldKey);
                boolean teamsFree = !busy.contains(match.getHomeTeam().getId())
                        && !busy.contains(match.getAwayTeam().getId());

                if (fieldFree && teamsFree) {
                    match.setMatchDate(date);
                    match.setMatchTime(time);
                    match.setField(field);
                    fieldOccupied.add(fieldKey);
                    busy.add(match.getHomeTeam().getId());
                    busy.add(match.getAwayTeam().getId());
                    assigned = true;
                }
                slotCursor++;
                attempts++;
                if (attempts > maxSlots) break;
            }
        }
    }

    /**
     * Vérifie si un créneau donné entre en conflit avec une liste de matchs.
     * Un conflit survient si le terrain est déjà utilisé, ou si l'une des deux
     * équipes joue déjà à ce créneau.
     */
    public static boolean hasConflict(Team home, Team away, LocalDate date, LocalTime time,
                                      String field, List<Match> existing, Long excludeMatchId) {
        if (date == null || time == null) return false;
        for (Match other : existing) {
            if (excludeMatchId != null && excludeMatchId.equals(other.getId())) continue;
            if (other.getMatchDate() == null || other.getMatchTime() == null) continue;
            if (!other.getMatchDate().equals(date) || !other.getMatchTime().equals(time)) continue;

            boolean sameField = field != null && field.equalsIgnoreCase(other.getField());
            boolean teamClash = sameTeam(home, other) || sameTeam(away, other);
            if (sameField || teamClash) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameTeam(Team team, Match match) {
        if (team == null) return false;
        return team.equals(match.getHomeTeam()) || team.equals(match.getAwayTeam());
    }
}
