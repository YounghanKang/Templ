package com.example.demo.collaboration;

import com.example.demo.job.JobStatus;
import com.example.demo.job.JobType;
import com.example.demo.job.ProcessingJob;
import com.example.demo.job.ProcessingJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CollaborationEventIngestionService.class)
class CollaborationEventIngestionServiceTest {

    @Autowired
    private CollaborationEventIngestionService ingestionService;

    @Autowired
    private CollaborationEventRepository eventRepository;

    @Autowired
    private ProcessingJobRepository jobRepository;


    @Test
    void eventAndProcessingJobAreStoredTogether() {

        String projectId = UUID.randomUUID().toString().toString();

        var result = ingestionService.ingest(
                projectId,
                SourceTool.SLACK,
                CollaborationEventType.MESSAGE,
                EventAction.CREATED,
                "Ev-test-001",
                null,
                "로그인 API 응답 필드를 변경합니다.",
                "U001",
                "테스트 사용자",
                "#backend",
                null,
                false,
                Instant.now()
        );

        assertFalse(result.duplicate());
        assertNotNull(result.eventId());
        assertNotNull(result.jobId());


        CollaborationEvent savedEvent =
                eventRepository
                        .findById(result.eventId())
                        .orElseThrow();

        assertEquals(
                projectId,
                savedEvent.getProjectId()
        );

        assertEquals(
                SourceTool.SLACK,
                savedEvent.getSourceTool()
        );


        ProcessingJob savedJob =
                jobRepository
                        .findById(result.jobId())
                        .orElseThrow();

        assertEquals(
                result.eventId(),
                savedJob.getCollaborationEventId()
        );

        assertEquals(
                JobType.FILTER_EVENT,
                savedJob.getJobType()
        );

        assertEquals(
                JobStatus.READY,
                savedJob.getStatus()
        );
    }
    @Test
    void duplicatedExternalEventIsNotStoredTwice() {

        String projectId = UUID.randomUUID().toString().toString();

        var first = ingestionService.ingest(
                projectId,
                SourceTool.SLACK,
                CollaborationEventType.MESSAGE,
                EventAction.CREATED,
                "Ev-test-duplicate",
                null,
                "로그인 API를 변경합니다.",
                "U001",
                "테스트 사용자",
                "#backend",
                null,
                false,
                Instant.now()
        );

        var second = ingestionService.ingest(
                projectId,
                SourceTool.SLACK,
                CollaborationEventType.MESSAGE,
                EventAction.CREATED,
                "Ev-test-duplicate",
                null,
                "로그인 API를 변경합니다.",
                "U001",
                "테스트 사용자",
                "#backend",
                null,
                false,
                Instant.now()
        );

        assertFalse(first.duplicate());
        assertTrue(second.duplicate());

        assertNotNull(first.eventId());
        assertNotNull(first.jobId());

        assertNull(second.eventId());
        assertNull(second.jobId());

        assertEquals(
                1,
                eventRepository.count()
        );

        assertEquals(
                1,
                jobRepository.count()
        );
    }
    @Test
    void sameExternalEventCanBeStoredForDifferentProjects() {

        String projectAId = UUID.randomUUID().toString().toString();
        String projectBId = UUID.randomUUID().toString().toString();

        String sameExternalSourceId =
                "Ev-shared-001";

        var projectAResult =
                ingestionService.ingest(
                        projectAId,
                        SourceTool.SLACK,
                        CollaborationEventType.MESSAGE,
                        EventAction.CREATED,
                        sameExternalSourceId,
                        null,
                        "로그인 API 범위를 변경합니다.",
                        "U001",
                        "테스트 사용자",
                        "#backend",
                        null,
                        false,
                        Instant.now()
                );

        var projectBResult =
                ingestionService.ingest(
                        projectBId,
                        SourceTool.SLACK,
                        CollaborationEventType.MESSAGE,
                        EventAction.CREATED,
                        sameExternalSourceId,
                        null,
                        "로그인 API 범위를 변경합니다.",
                        "U001",
                        "테스트 사용자",
                        "#backend",
                        null,
                        false,
                        Instant.now()
                );

        assertFalse(projectAResult.duplicate());
        assertFalse(projectBResult.duplicate());

        assertNotNull(projectAResult.eventId());
        assertNotNull(projectBResult.eventId());

        assertNotEquals(
                projectAResult.eventId(),
                projectBResult.eventId()
        );

        assertEquals(
                2,
                eventRepository.count()
        );

        assertEquals(
                2,
                jobRepository.count()
        );
    }
}