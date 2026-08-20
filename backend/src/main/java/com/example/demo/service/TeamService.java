package com.example.demo.service;

import com.example.demo.domain.Team;
import com.example.demo.dto.TeamDto;
import com.example.demo.dto.UserDto;
import com.example.demo.repository.TeamRepository;
import com.example.demo.repository.RoadmapNodeRepository;
import com.example.demo.repository.RoadmapEdgeRepository;
import com.example.demo.repository.SpecificationRepository;
import com.example.demo.repository.SuggestionRepository;
import com.example.demo.domain.TeamMember;
import com.example.demo.domain.User;
import com.example.demo.repository.TeamMemberRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final SpecificationRepository specificationRepository;
    private final SuggestionRepository suggestionRepository;
    private final RoadmapNodeRepository roadmapNodeRepository;
    private final RoadmapEdgeRepository roadmapEdgeRepository;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       UserRepository userRepository,
                       SpecificationRepository specificationRepository, 
                       SuggestionRepository suggestionRepository,
                       RoadmapNodeRepository roadmapNodeRepository,
                       RoadmapEdgeRepository roadmapEdgeRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.specificationRepository = specificationRepository;
        this.suggestionRepository = suggestionRepository;
        this.roadmapNodeRepository = roadmapNodeRepository;
        this.roadmapEdgeRepository = roadmapEdgeRepository;
    }

    public List<TeamDto.TeamResponse> listTeams() {
        return teamRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public List<TeamDto.TeamResponse> listTeams(String username) {
        if (username == null || username.isBlank()) {
            return listTeams();
        }

        Set<String> teamIds = new LinkedHashSet<>();
        // 1. Teams where user is an explicit member
        List<TeamMember> members = teamMemberRepository.findByUsername(username);
        for (TeamMember m : members) {
            teamIds.add(m.getTeamId());
        }

        // 2. Teams where user is owner
        List<Team> ownedTeams = teamRepository.findAllByOwnerUsernameOrderByIdAsc(username);
        for (Team t : ownedTeams) {
            teamIds.add(t.getTeamId());
        }

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return teamRepository.findByTeamIdInOrderByIdAsc(new ArrayList<>(teamIds)).stream()
                .map(this::toDto)
                .toList();
    }

    public TeamDto.TeamResponse createTeam(TeamDto.CreateTeamRequest request) {
        return createTeam(null, request);
    }

    @Transactional
    public TeamDto.TeamResponse createTeam(String username, TeamDto.CreateTeamRequest request) {
        String id = generateTeamId();
        Team team = Team.builder()
                .teamId(id)
                .ownerUsername(username)
                .name(request.getName() == null || request.getName().isBlank() ? "New Team" : request.getName())
                .members(request.getMembers() == null ? 1 : request.getMembers())
                .color(request.getColor() == null || request.getColor().isBlank() ? "#6b5cf6" : request.getColor())
                .mission(request.getMission() == null ? "" : request.getMission())
                .slackHandle(request.getSlackHandle())
                .githubRepo(request.getGithubRepo())
                .build();
        Team saved = teamRepository.save(team);

        if (username != null && !username.isBlank()) {
            User user = userRepository.findByUsername(username).orElse(null);
            TeamMember member = TeamMember.builder()
                    .teamId(id)
                    .username(username)
                    .userEmail(user != null ? user.getEmail() : null)
                    .nickname(user != null && user.getNickname() != null ? user.getNickname() : username)
                    .role("OWNER")
                    .joinedAt(Instant.now())
                    .build();
            teamMemberRepository.save(member);
        }

        return toDto(saved);
    }

    public TeamDto.TeamResponse getTeam(String teamId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        return toDto(team);
    }

    @Transactional
    public void deleteTeam(String teamId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        
        // cascade delete
        roadmapNodeRepository.findAllByTeamIdOrderByIdAsc(teamId).forEach(roadmapNodeRepository::delete);
        roadmapEdgeRepository.findAllByTeamIdOrderByIdAsc(teamId).forEach(roadmapEdgeRepository::delete);
        specificationRepository.findAllByTeamIdOrderByIdDesc(teamId).forEach(specificationRepository::delete);
        suggestionRepository.findAllByTeamIdOrderByIdDesc(teamId).forEach(suggestionRepository::delete);
        teamMemberRepository.deleteAllByTeamId(teamId);
        
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
