package com.example.demo.repository;

import com.example.demo.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecificationRepository extends JpaRepository<Specification, Long> {
    List<Specification> findAllByTeamIdOrderByIdDesc(String teamId);
}
