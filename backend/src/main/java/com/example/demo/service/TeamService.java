package com.example.demo.service;

import com.example.demo.domain.Team;
import com.example.demo.dto.TeamDto;
import com.example.demo.dto.UserDto;
import com.example.demo.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }



    public List<TeamDto.TeamResponse> listTeams() {
        return teamRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public TeamDto.TeamResponse createTeam(TeamDto.CreateTeamRequest request) {
        String id = generateTeamId();
        Team team = Team.builder()
                .teamId(id)
                .name(request.getName() == null || request.getName().isBlank() ? "New Team" : request.getName())
                .members(request.getMembers() == null ? 1 : request.getMembers())
                .color(request.getColor() == null || request.getColor().isBlank() ? "#6b5cf6" : request.getColor())
                .mission(request.getMission() == null ? "" : request.getMission())
                .slackHandle(request.getSlackHandle())
                .githubRepo(request.getGithubRepo())
                .build();
        return toDto(teamRepository.save(team));
    }

    public TeamDto.TeamResponse getTeam(String teamId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        return toDto(team);
    }

    public void deleteTeam(String teamId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        teamRepository.delete(team);
    }

    public TeamDto.TeamResponse updateTeam(String teamId, TeamDto.UpdateTeamRequest request) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        if (request.getName() != null) team.setName(request.getName());
        if (request.getColor() != null) team.setColor(request.getColor());
        if (request.getMission() != null) team.setMission(request.getMission());
        if (request.getSlackHandle() != null) team.setSlackHandle(request.getSlackHandle());
        if (request.getGithubRepo() != null) team.setGithubRepo(request.getGithubRepo());
        return toDto(teamRepository.save(team));
    }

    public UserDto.IntegrationSettingsDto getIntegrations(String teamId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        return UserDto.IntegrationSettingsDto.builder()
                .slack(new UserDto.AccountDto(
                        team.getSlackHandle() != null && !team.getSlackHandle().isBlank(),
                        team.getSlackHandle() == null ? "" : team.getSlackHandle()))
                .github(new UserDto.AccountDto(
                        team.getGithubRepo() != null && !team.getGithubRepo().isBlank(),
                        team.getGithubRepo() == null ? "" : team.getGithubRepo()))
                .build();
    }

    public UserDto.IntegrationSettingsDto updateIntegrations(String teamId, UserDto.IntegrationSettingsDto request) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        if (request.getSlack() != null) {
            team.setSlackHandle(request.getSlack().getHandle());
        }
        if (request.getGithub() != null) {
            team.setGithubRepo(request.getGithub().getHandle());
        }
        teamRepository.save(team);
        return getIntegrations(teamId);
    }

    private String generateTeamId() {
        long count = teamRepository.count() + 1;
        String id = "T-" + String.format("%03d", count);
        int attempt = 1;
        while (teamRepository.existsByTeamId(id)) {
            count = teamRepository.count() + attempt + 1;
            id = "T-" + String.format("%03d", count);
            attempt++;
        }
        return id;
    }

    private TeamDto.TeamResponse toDto(Team team) {
        return TeamDto.TeamResponse.builder()
                .id(team.getTeamId())
                .name(team.getName())
                .members(team.getMembers())
                .color(team.getColor())
                .mission(team.getMission())
                .slackHandle(team.getSlackHandle())
                .githubRepo(team.getGithubRepo())
                .build();
    }
}
