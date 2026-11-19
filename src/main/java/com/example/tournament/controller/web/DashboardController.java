package com.example.tournament.controller.web;

import com.example.tournament.dto.response.BracketResponse;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.TournamentType;
import com.example.tournament.service.MatchService;
import com.example.tournament.service.RankingService;
import com.example.tournament.service.TeamService;
import com.example.tournament.service.TournamentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class DashboardController {

    private final TournamentService tournamentService;
    private final MatchService matchService;
    private final RankingService rankingService;
    private final TeamService teamService;

    public DashboardController(TournamentService tournamentService,
                               MatchService matchService,
                               RankingService rankingService,
                               TeamService teamService) {
        this.tournamentService = tournamentService;
        this.matchService = matchService;
        this.rankingService = rankingService;
        this.teamService = teamService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<TournamentResponse> tournaments = tournamentService.getAllTournaments();
        long activeCount = tournaments.stream()
                .filter(t -> t.getStatus() == TournamentStatus.EN_COURS
                        || t.getStatus() == TournamentStatus.PROGRAMME)
                .count();
        model.addAttribute("tournaments", tournaments);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("teamCount", teamService.getAllTeams().size());
        model.addAttribute("currentPage", "dashboard");
        return "dashboard";
    }

    @GetMapping("/tournaments/{id}")
    public String tournamentDetail(@PathVariable Long id, Model model) {
        TournamentResponse tournament = tournamentService.getTournamentById(id);
        List<MatchResponse> matches = matchService.getMatchesByTournament(id);
        List<StandingResponse> ranking = rankingService.getRankingByTournament(id);
        List<TeamResponse> availableTeams = teamService.getAvailableTeams(id);
        List<TeamResponse> registeredTeams = teamService.getTeamsOfTournament(id);

        model.addAttribute("tournament", tournament);
        model.addAttribute("matches", matches);
        model.addAttribute("ranking", ranking);
        model.addAttribute("availableTeams", availableTeams);
        model.addAttribute("registeredTeams", registeredTeams);

        boolean poules = tournament.getType() == TournamentType.POULES_ELIMINATION;
        boolean hasGroupMatches = matches.stream()
                .anyMatch(m -> m.getPhase() == com.example.tournament.entity.MatchPhase.GROUP_STAGE);
        boolean groupStageComplete = hasGroupMatches && matches.stream()
                .filter(m -> m.getPhase() == com.example.tournament.entity.MatchPhase.GROUP_STAGE)
                .allMatch(m -> m.getStatus() == com.example.tournament.entity.MatchStatus.COMPLETED);
        boolean eliminationGenerated = matches.stream()
                .anyMatch(m -> m.getPhase() != com.example.tournament.entity.MatchPhase.GROUP_STAGE);
        model.addAttribute("poules", poules);
        model.addAttribute("groupStageComplete", groupStageComplete);
        model.addAttribute("eliminationGenerated", eliminationGenerated);

        // Qualification des 3 premiers vers la Ligue des Champions : possible
        // uniquement pour un tournoi terminé (autre que la LdC elle-même).
        boolean canQualify = tournament.getStatus() == TournamentStatus.TERMINE
                && ranking.size() >= 3
                && !"Ligue des Champions".equalsIgnoreCase(tournament.getName());
        model.addAttribute("canQualify", canQualify);

        if (tournament.getType() == TournamentType.ELIMINATION_DIRECTE
                || (poules && eliminationGenerated)) {
            BracketResponse bracket = matchService.getBracket(id);
            model.addAttribute("bracket", bracket);
        }

        model.addAttribute("currentPage", "tournaments");
        return "tournament-detail";
    }
}
