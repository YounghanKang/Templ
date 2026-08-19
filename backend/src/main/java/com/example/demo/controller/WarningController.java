package com.example.demo.controller;

import com.example.demo.analysis.WarningQueryService;
import com.example.demo.analysis.WarningResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BE2가 생성한 협업 진행상황 경고를 조회하는 API입니다.
 *
 * 이 Controller는 분석 결과를 조회만 하며
 * 로드맵/Task를 자동 수정하지 않습니다.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/warnings")
public class WarningController {

    private final WarningQueryService warningQueryService;

    public WarningController(
            WarningQueryService warningQueryService
    ) {
        this.warningQueryService = warningQueryService;
    }

    @GetMapping
    public ResponseEntity<List<WarningResponse>> listWarnings(
            @PathVariable String projectId
    ) {

        return ResponseEntity.ok(
                warningQueryService.findWarnings(projectId)
        );
    }
}
