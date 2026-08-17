package com.example.demo.repository;

import com.example.demo.domain.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {
    List<Suggestion> findAllByTeamIdOrderByIdDesc(String teamId);
    boolean existsBySourceIdAndStatus(String sourceId, String status);
}
