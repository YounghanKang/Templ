package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.dto.TeamDto;
import com.example.demo.repository.TeamMemberRepository;
import com.example.demo.repository.TeamRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TeamServiceUserIsolationTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @BeforeEach
    void setUp() {
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자별로 본인이 속한 팀만 조회되어야 한다")
    void testUserIsolation() {
        // Given
        String userA = "userA";
        String userB = "userB";

        userRepository.save(User.builder().username(userA).password("pass").email("a@test.com").nickname("UserA").build());
        userRepository.save(User.builder().username(userB).password("pass").email("b@test.com").nickname("UserB").build());

        // User A creates Team 1
        TeamDto.TeamResponse teamA = teamService.createTeam(userA, TeamDto.CreateTeamRequest.builder()
                .name("Team of User A")
                .color("#6b5cf6")
                .mission("User A's mission")
                .build());

        // User B creates Team 2
        TeamDto.TeamResponse teamB = teamService.createTeam(userB, TeamDto.CreateTeamRequest.builder()
                .name("Team of User B")
                .color("#ff0000")
                .mission("User B's mission")
                .build());

        // When
        List<TeamDto.TeamResponse> userATeams = teamService.listTeams(userA);
        List<TeamDto.TeamResponse> userBTeams = teamService.listTeams(userB);

        // Then
        assertThat(userATeams).hasSize(1);
        assertThat(userATeams.get(0).getName()).isEqualTo("Team of User A");
        assertThat(userATeams.get(0).getId()).isEqualTo(teamA.getId());

        assertThat(userBTeams).hasSize(1);
        assertThat(userBTeams.get(0).getName()).isEqualTo("Team of User B");
        assertThat(userBTeams.get(0).getId()).isEqualTo(teamB.getId());
    }

    @Test
    @DisplayName("신규 계정은 생성된 팀이 없으면 빈 목록을 반환한다")
    void testNewUserHasNoTeams() {
        String newUser = "google_new_user";
        List<TeamDto.TeamResponse> teams = teamService.listTeams(newUser);
        assertThat(teams).isEmpty();
    }
}
