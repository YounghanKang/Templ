package com.example.demo.matching;

import java.util.List;
import java.util.UUID;

public interface ProjectContextReader {

    ProjectContext getProjectContext(
            String projectId
    );

    List<NodeContext> findCandidateNodes(
            String projectId,
            String channelId
    );

    List<NodeRelation> getRelatedNodes(
            String nodeId
    );
}