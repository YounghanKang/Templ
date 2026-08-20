package com.example.demo.repository;

import com.example.demo.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUsername(String username);
    List<TeamMember> findByTeamId(String teamId);
    Optional<TeamMember> findByTeamIdAndUsername(String teamId, String username);
    boolean existsByTeamIdAndUsername(String teamId, String username);
    
    @Transactional
    void deleteAllByTeamId(String teamId);
}
