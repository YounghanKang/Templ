package com.example.demo.service;

import com.example.demo.domain.Team;
import com.example.demo.dto.TeamDto;
import com.example.demo.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
        seedDefaultTeams();
    }

    private void seedDefaultTeams() {
        if (teamRepository.count() > 0) {
            return;
        }

        teamRepository.saveAll(List.of(
                Team.builder().teamId("T-001").name("Design System").members(8).color("#6b5cf6").mission("모든 사용자가 직관적으로 제품을 사용할 수 있도록 일관된 디자인 언어를 구축한다.").build(),
                Team.builder().teamId("T-002").name("Platform Engineering").members(14).color("#10b981").mission("개발자 경험을 최우선으로, 확장 가능하고 안정적인 인프라 기반을 마련한다.").build(),
                Team.builder().teamId("T-003").name("Growth & Marketing").members(6).color("#f59e0b").mission("데이터 기반 실험으로 제품 성장을 가속화하고 시장 점유율을 확대한다.").build()
        ));
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
                .build();
        return toDto(teamRepository.save(team));
    }

    public TeamDto.TeamResponse getTeam(String teamId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        return toDto(team);
    }

    public TeamDto.TeamResponse updateTeam(String teamId, TeamDto.UpdateTeamRequest request) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
        if (request.getName() != null) team.setName(request.getName());
        if (request.getColor() != null) team.setColor(request.getColor());
        if (request.getMission() != null) team.setMission(request.getMission());
        return toDto(teamRepository.save(team));
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
                .build();
    }
}
