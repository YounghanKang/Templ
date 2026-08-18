package com.example.demo.controller;

import com.example.demo.dto.EventFilterDto;
import com.example.demo.service.EventFilterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams/{teamId}/events")
public class EventFilterController {

    private final EventFilterService eventFilterService;

    public EventFilterController(EventFilterService eventFilterService) {
        this.eventFilterService = eventFilterService;
    }

    @PostMapping("/filter")
    public ResponseEntity<EventFilterDto.FilterResultResponse> evaluate(
            @PathVariable String teamId,
            @RequestBody EventFilterDto.FilterRequest request) {
        return ResponseEntity.ok(eventFilterService.evaluate(teamId, request));
    }
}
