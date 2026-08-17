package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TeamDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamResponse {
        private String id;
        private String name;
        private Integer members;
        private String color;
        private String mission;
        private String slackHandle;
        private String githubRepo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateTeamRequest {
        private String name;
        private Integer members;
        private String color;
        private String mission;
        private String slackHandle;
        private String githubRepo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateTeamRequest {
        private String id;
        private String name;
        private String color;
        private String mission;
        private String slackHandle;
        private String githubRepo;
    }
}
