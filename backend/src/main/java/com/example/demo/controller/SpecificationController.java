package com.example.demo.controller;

import com.example.demo.dto.SpecificationDto;
import com.example.demo.dto.SuggestionDto;
import com.example.demo.service.SpecificationService;
import com.example.demo.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{teamId}/specs")
public class SpecificationController {

    private final SpecificationService specificationService;
    private final SuggestionService suggestionService;

    public SpecificationController(SpecificationService specificationService, SuggestionService suggestionService) {
        this.specificationService = specificationService;
        this.suggestionService = suggestionService;
    }

    @PostMapping
    public ResponseEntity<SpecificationDto.Response> submit(@PathVariable String teamId, @RequestBody SpecificationDto.Create req) {
        return ResponseEntity.ok(specificationService.submit(teamId, req));
    }

    @GetMapping
    public ResponseEntity<List<SpecificationDto.Response>> list(@PathVariable String teamId) {
        return ResponseEntity.ok(specificationService.listForTeam(teamId));
    }

    @GetMapping("/{specId}/suggestions")
    public ResponseEntity<List<SuggestionDto.Response>> suggestions(@PathVariable String teamId, @PathVariable Long specId) {
        // filter suggestions by sourceId == specId
        List<SuggestionDto.Response> all = suggestionService.listForTeam(teamId);
        List<SuggestionDto.Response> filtered = all.stream().filter(s -> s.getSourceId() != null && s.getSourceId().equals(String.valueOf(specId))).toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{specId}/preview")
    public ResponseEntity<com.example.demo.dto.RoadmapDto.RoadmapGraphDto> preview(@PathVariable String teamId, @PathVariable Long specId) {
        var graph = specificationService.generatePreviewGraph(teamId, specId);
        return ResponseEntity.ok(graph);
    }
}
