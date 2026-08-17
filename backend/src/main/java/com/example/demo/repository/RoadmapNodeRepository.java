package com.example.demo.repository;

import com.example.demo.domain.RoadmapNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoadmapNodeRepository extends JpaRepository<RoadmapNode, Long> {
    List<RoadmapNode> findAllByTeamIdOrderByIdAsc(String teamId);
    Optional<RoadmapNode> findByNodeIdAndTeamId(String nodeId, String teamId);
    void deleteByNodeIdAndTeamId(String nodeId, String teamId);
}
