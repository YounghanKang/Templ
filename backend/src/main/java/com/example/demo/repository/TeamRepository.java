package com.example.demo.repository;

import com.example.demo.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByTeamId(String teamId);
    boolean existsByTeamId(String teamId);
    List<Team> findAllByOrderByIdAsc();
    List<Team> findAllByOwnerUsernameOrderByIdAsc(String ownerUsername);
    List<Team> findByTeamIdInOrderByIdAsc(List<String> teamIds);
}
