package com.example.demo.controller;

import com.example.demo.dto.SuggestionDto;
import com.example.demo.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{teamId}/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public ResponseEntity<List<SuggestionDto.Response>> list(@PathVariable String teamId) {
        return ResponseEntity.ok(suggestionService.listForTeam(teamId));
    }

    @PostMapping
    public ResponseEntity<SuggestionDto.Response> create(@PathVariable String teamId, @RequestBody SuggestionDto.Create req) {
        return ResponseEntity.ok(suggestionService.create(teamId, req));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SuggestionDto.Response> approve(@PathVariable String teamId, @PathVariable Long id, @RequestParam(required = false, defaultValue = "system") String by) {
        return ResponseEntity.ok(suggestionService.approve(teamId, id, by));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SuggestionDto.Response> reject(@PathVariable String teamId, @PathVariable Long id, @RequestParam(required = false, defaultValue = "system") String by) {
        return ResponseEntity.ok(suggestionService.reject(teamId, id, by));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<List<SuggestionDto.Response>> regenerate(
            @PathVariable String teamId,
            @PathVariable Long id,
            @RequestBody(required = false) SuggestionDto.RegenerateRequest req) {
        return ResponseEntity.ok(suggestionService.regenerate(teamId, id, req));
    }
}
