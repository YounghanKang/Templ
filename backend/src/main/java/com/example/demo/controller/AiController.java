package com.example.demo.controller;

import com.example.demo.dto.SuggestionDto;
import com.example.demo.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/test")
    public ResponseEntity<List<SuggestionDto.Response>> test(@RequestBody TestRequest req) {
        List<SuggestionDto.Create> generated = aiService.generateSuggestions(
                req.teamId == null ? "T-TEST" : req.teamId,
                req.specId,
                req.specText,
                req.feedback,
                req.baseSuggestionId
        );
        List<SuggestionDto.Response> resp = generated.stream().map(c -> SuggestionDto.Response.builder()
                .id(null)
                .teamId(req.teamId)
                .targetType(c.getTargetType())
                .targetId(c.getTargetId())
                .title(c.getTitle())
                .body(c.getBody())
                .sourceTool(c.getSourceTool())
                .sourceId(c.getSourceId())
                .changeJson(c.getChangeJson())
                .status("PREVIEW")
                .createdAt(Instant.now())
                .build()).collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    public static class TestRequest {
        public String teamId;
        public Long specId;
        public String specText;
        public String feedback;
        public Long baseSuggestionId;
    }
}
