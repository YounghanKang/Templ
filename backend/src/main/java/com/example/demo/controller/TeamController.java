package com.example.demo.controller;

import com.example.demo.dto.TeamDto;
import com.example.demo.dto.UserDto;
import com.example.demo.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamDto.TeamResponse>> getTeams(HttpServletRequest request) {
        String username = (String) request.getAttribute("authUser");
        return ResponseEntity.ok(teamService.listTeams(username));
    }

    @PostMapping("/teams")
    public ResponseEntity<TeamDto.TeamResponse> createTeam(@RequestBody TeamDto.CreateTeamRequest request,
                                                           HttpServletRequest httpRequest) {
        String username = (String) httpRequest.getAttribute("authUser");
        return ResponseEntity.ok(teamService.createTeam(username, request));
    }

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<TeamDto.TeamResponse> getTeam(@PathVariable String teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    @PatchMapping("/teams/{teamId}")
    public ResponseEntity<TeamDto.TeamResponse> updateTeam(@PathVariable String teamId,
                                                         @RequestBody TeamDto.UpdateTeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(teamId, request));
    }

    @GetMapping("/teams/{teamId}/integrations")
    public ResponseEntity<UserDto.IntegrationSettingsDto> getIntegrations(@PathVariable String teamId) {
        return ResponseEntity.ok(teamService.getIntegrations(teamId));
    }

    @PatchMapping("/teams/{teamId}/integrations")
    public ResponseEntity<UserDto.IntegrationSettingsDto> updateIntegrations(@PathVariable String teamId,
                                                                            @RequestBody UserDto.IntegrationSettingsDto request) {
        return ResponseEntity.ok(teamService.updateIntegrations(teamId, request));
    }

    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.noContent().build();
    }
}
