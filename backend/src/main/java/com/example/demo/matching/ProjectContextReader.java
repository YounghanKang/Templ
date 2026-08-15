package com.example.demo.matching;

import java.util.List;
import java.util.UUID;

public interface ProjectContextReader {

    ProjectContext getProjectContext(
            UUID projectId
    );

    List<NodeContext> findCandidateNodes(
            UUID projectId,
            String channelId
    );

    List<NodeRelation> getRelatedNodes(
            UUID nodeId
    );
}