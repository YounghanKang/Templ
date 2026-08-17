package com.example.demo.repository;

import com.example.demo.domain.RoadmapEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoadmapEdgeRepository extends JpaRepository<RoadmapEdge, Long> {
    List<RoadmapEdge> findAllByTeamIdOrderByIdAsc(String teamId);
    Optional<RoadmapEdge> findByEdgeIdAndTeamId(String edgeId, String teamId);
    void deleteByEdgeIdAndTeamId(String edgeId, String teamId);
}
