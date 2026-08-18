package com.example.demo.matching;

import java.util.List;

public record NodeContext(
        String nodeId,
        String title,
        String goal,
        String summary,
        String status,
        Integer progress,
        List<String> assignees,
        List<String> prerequisites,
        String dueDate,
        List<String> keywords
) {

    public NodeContext {

        assignees =
                assignees == null
                        ? List.of()
                        : List.copyOf(assignees);

        prerequisites =
                prerequisites == null
                        ? List.of()
                        : List.copyOf(prerequisites);

        keywords =
                keywords == null
                        ? List.of()
                        : List.copyOf(keywords);
    }


    /*
     * 기존 테스트와 기존 호출부가 사용하는
     * 5개 인자 생성자를 그대로 지원합니다.
     */
    public NodeContext(
            String nodeId,
            String title,
            String goal,
            String summary,
            List<String> keywords
    ) {

        this(
                nodeId,
                title,
                goal,
                summary,
                null,
                null,
                List.of(),
                List.of(),
                null,
                keywords
        );
    }
}