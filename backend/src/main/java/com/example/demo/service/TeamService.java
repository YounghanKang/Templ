package com.example.demo.service;

import com.example.demo.dto.TeamDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TeamService {

    private final List<TeamDto.TeamResponse> teams = new ArrayList<>();
    private final AtomicInteger counter = new AtomicInteger(1);

    public TeamService() {
        teams.add(new TeamDto.TeamResponse("T-001", "Design System", 8, "#6b5cf6", "모든 사용자가 직관적으로 제품을 사용할 수 있도록 일관된 디자인 언어를 구축한다."));
        teams.add(new TeamDto.TeamResponse("T-002", "Platform Engineering", 14, "#10b981", "개발자 경험을 최우선으로, 확장 가능하고 안정적인 인프라 기반을 마련한다."));
        teams.add(new TeamDto.TeamResponse("T-003", "Growth & Marketing", 6, "#f59e0b", "데이터 기반 실험으로 제품 성장을 가속화하고 시장 점유율을 확대한다."));
    }

    public List<TeamDto.TeamResponse> listTeams() {
        return teams;
    }

    public TeamDto.TeamResponse createTeam(TeamDto.CreateTeamRequest request) {
        String id = "T-" + String.format("%03d", counter.getAndIncrement());
        TeamDto.TeamResponse team = TeamDto.TeamResponse.builder()
                .id(id)
                .name(request.getName())
                .members(request.getMembers() == null ? 1 : request.getMembers())
                .color(request.getColor() == null || request.getColor().isBlank() ? "#6b5cf6" : request.getColor())
                .mission(request.getMission() == null ? "" : request.getMission())
                .build();
        teams.add(team);
        return team;
    }

    public TeamDto.TeamResponse getTeam(String teamId) {
        return teams.stream()
                .filter(team -> team.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("team not found: " + teamId));
    }

    public TeamDto.TeamResponse updateTeam(String teamId, TeamDto.UpdateTeamRequest request) {
        TeamDto.TeamResponse team = getTeam(teamId);
        if (request.getName() != null) {
            team.setName(request.getName());
        }
        if (request.getColor() != null) {
            team.setColor(request.getColor());
        }
        if (request.getMission() != null) {
            team.setMission(request.getMission());
        }
        return team;
    }
}
