package com.example.demo.controller;

import com.example.demo.dto.RoadmapDto;
import com.example.demo.service.RoadmapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams/{teamId}")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @GetMapping("/roadmap")
    public ResponseEntity<RoadmapDto.RoadmapGraphDto> getRoadmap(@PathVariable String teamId) {
        return ResponseEntity.ok(roadmapService.getRoadmapGraph(teamId));
    }

    @PostMapping("/nodes")
    public ResponseEntity<RoadmapDto.RoadmapNodeDto> createNode(@PathVariable String teamId,
                                                               @RequestBody RoadmapDto.RoadmapNodeDto request) {
        return ResponseEntity.ok(roadmapService.createNode(teamId, request));
    }

    @PatchMapping("/nodes/{nodeId}")
    public ResponseEntity<RoadmapDto.RoadmapNodeDto> updateNode(@PathVariable String teamId,
                                                               @PathVariable String nodeId,
                                                               @RequestBody RoadmapDto.RoadmapNodeDto request) {
        return ResponseEntity.ok(roadmapService.updateNode(teamId, nodeId, request));
    }

    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<Void> deleteNode(@PathVariable String teamId, @PathVariable String nodeId) {
        roadmapService.deleteNode(teamId, nodeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/edges")
    public ResponseEntity<RoadmapDto.RoadmapEdgeDto> createEdge(@PathVariable String teamId,
                                                               @RequestBody RoadmapDto.RoadmapEdgeDto request) {
        return ResponseEntity.ok(roadmapService.createEdge(teamId, request));
    }

    @DeleteMapping("/edges/{edgeId}")
    public ResponseEntity<Void> deleteEdge(@PathVariable String teamId, @PathVariable String edgeId) {
        roadmapService.deleteEdge(teamId, edgeId);
        return ResponseEntity.noContent().build();
    }
}
