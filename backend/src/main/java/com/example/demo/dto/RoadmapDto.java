package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class RoadmapDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentDto {
        private Long id;
        private String author;
        private String text;
        private String time;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileDto {
        private Long id;
        private String name;
        private String size;
        private String author;
        private String date;
        private String url;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeDetailDto {
        private String label;
        private String code;
        private String status;
        private Integer progress;
        private String goal;
        private String dueDate;
        private List<String> assignees;
        private List<String> prerequisites;
        private String aiSummary;
        private String issue;
        private List<FileDto> files;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadmapNodeDto {
        private String id;
        private String label;
        private String code;
        private String status;
        private Integer progress;
        private String goal;
        private String dueDate;
        private List<String> assignees;
        private List<String> prerequisites;
        private String aiSummary;
        private String issue;
        private List<FileDto> files;
        private String tier;
        private Integer x;
        private Integer y;
        private Integer w;
        private Integer h;
        private List<CommentDto> comments;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadmapEdgeDto {
        private String id;
        private String from;
        private String to;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadmapGraphDto {
        private List<RoadmapNodeDto> nodes;
        private List<RoadmapEdgeDto> edges;
    }
}
