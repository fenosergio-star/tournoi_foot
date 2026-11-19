package com.example.tournament.controller.rest;

import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
public class RankingRestController {

    private final RankingService rankingService;

    public RankingRestController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/tournaments/{id}")
    public ResponseEntity<List<StandingResponse>> getRanking(@PathVariable Long id) {
        return ResponseEntity.ok(rankingService.getRankingByTournament(id));
    }
}
