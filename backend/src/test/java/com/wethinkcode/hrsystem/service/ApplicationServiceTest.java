package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.config.RabbitMQConfig;
import com.wethinkcode.hrsystem.dto.ApplicationOutcomeChangedEvent;
import com.wethinkcode.hrsystem.dto.ApplicationOutcomeRequest;
import com.wethinkcode.hrsystem.dto.ApplicationRequest;
import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.ApplicationDocumentRepository;
import com.wethinkcode.hrsystem.repository.ApplicationRepository;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private ApplicationDocumentRepository applicationDocumentRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @TempDir
    Path tempDir;

    private ApplicationService applicationService;

    private JobPosting openPosting;
    private ApplicationRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        applicationService = new ApplicationService(
                applicationRepository, jobPostingRepository, applicationDocumentRepository, rabbitTemplate,
                tempDir.resolve("applications").toString() + "/",
                tempDir.resolve("application-documents").toString() + "/");

        openPosting = new JobPosting();
        openPosting.setId(1L);
        openPosting.setTitle("Backend Developer");
        openPosting.setStartDate(LocalDate.now().minusDays(1));
        openPosting.setEndDate(LocalDate.now().plusDays(10));
        openPosting.setMaxApplications(10);
        openPosting.setRequiredDocuments(List.of());

        request = new ApplicationRequest();
        request.setCandidateName("Jane Candidate");
        request.setCandidateEmail("jane@example.com");
        request.setCandidatePhone("0821234567");
        request.setCoverLetter("I am a great fit");
    }

    // ---------- submit() ----------

    @Test
    void submit_postingNotFound_throwsException() {
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> applicationService.submit(1L, request, null, Map.of()));
    }

    @Test
    void submit_notYetOpen_throwsException() {
        openPosting.setStartDate(LocalDate.now().plusDays(5));
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(openPosting));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.submit(1L, request, null, Map.of()));
        assertTrue(ex.getMessage().contains("not yet open"));
    }

    @Test
    void submit_closed_throwsException() {
        openPosting.setEndDate(LocalDate.now().minusDays(1));
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(openPosting));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.submit(1L, request, null, Map.of()));
        assertTrue(ex.getMessage().contains("closed"));
    }

    @Test
    void submit_atMaxApplications_throwsException() {
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(openPosting));
        when(applicationRepository.countByJobPostingId(1L)).thenReturn(10L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.submit(1L, request, null, Map.of()));
        assertTrue(ex.getMessage().contains("maximum number"));
    }

    @Test
    void submit_missingRequiredDocument_throwsException() {
        openPosting.setRequiredDocuments(List.of(DocumentType.CV));
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(openPosting));
        when(applicationRepository.countByJobPostingId(1L)).thenReturn(0L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.submit(1L, request, null, Map.of()));
        assertTrue(ex.getMessage().contains("Missing required document"));
        verify(applicationRepository, never()).save(any()); // no save call made
    }

    @Test
    void submit_success_noCvNoDocuments() {
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(openPosting));
        when(applicationRepository.countByJobPostingId(1L)).thenReturn(0L);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(50L);
            return app;
        });
        when(applicationDocumentRepository.findByApplicationId(50L)).thenReturn(List.of());

        Application result = applicationService.submit(1L, request, null, Map.of());

        assertEquals("Jane Candidate", result.getCandidateName());
        assertEquals("SUBMITTED", result.getStatus());
        assertNull(result.getCvPath());
        assertNotNull(result.getSubmittedAt());
    }

    @Test
    void submit_success_withCv_savesFile() {
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(openPosting));
        when(applicationRepository.countByJobPostingId(1L)).thenReturn(0L);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(51L);
            return app;
        });
        when(applicationDocumentRepository.findByApplicationId(51L)).thenReturn(List.of());

        MockMultipartFile cv = new MockMultipartFile("cv", "resume.pdf", "application/pdf", "content".getBytes());

        Application result = applicationService.submit(1L, request, cv, Map.of());

        assertNotNull(result.getCvPath());
        assertTrue(result.getCvPath().endsWith(".pdf"));
    }

    @Test
    void submit_withRequiredDocumentsProvided_savesEachDocument() {
        openPosting.setRequiredDocuments(List.of(DocumentType.CV, DocumentType.ID_COPY));
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(openPosting));
        when(applicationRepository.countByJobPostingId(1L)).thenReturn(0L);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(52L);
            return app;
        });
        when(applicationDocumentRepository.save(any(ApplicationDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationDocumentRepository.findByApplicationId(52L)).thenReturn(List.of(new ApplicationDocument(), new ApplicationDocument()));

        MockMultipartFile cvDoc = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "content".getBytes());
        MockMultipartFile idDoc = new MockMultipartFile("id", "id.png", "image/png", "content".getBytes());

        Map<DocumentType, MultipartFile> docs = new HashMap<>();
        docs.put(DocumentType.CV, cvDoc);
        docs.put(DocumentType.ID_COPY, idDoc);

        Application result = applicationService.submit(1L, request, null, docs);

        verify(applicationDocumentRepository, times(2)).save(any(ApplicationDocument.class));
        assertEquals(2, result.getDocuments().size());
    }

    // ---------- getDocuments() / getById() / getByJobPosting() / getByStatus() / getAll() ----------

    @Test
    void getDocuments_delegatesToRepository() {
        when(applicationDocumentRepository.findByApplicationId(5L)).thenReturn(List.of(new ApplicationDocument()));

        assertEquals(1, applicationService.getDocuments(5L).size());
    }

    @Test
    void getById_found_returnsApplication() {
        Application app = new Application();
        app.setId(5L);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));

        assertEquals(app, applicationService.getById(5L));
    }

    @Test
    void getById_notFound_throwsException() {
        when(applicationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> applicationService.getById(9L));
    }

    @Test
    void getByJobPosting_delegatesToRepository() {
        when(applicationRepository.findByJobPostingId(1L)).thenReturn(List.of(new Application()));

        assertEquals(1, applicationService.getByJobPosting(1L).size());
    }

    @Test
    void getByStatus_delegatesToRepository() {
        when(applicationRepository.findByStatus("SUBMITTED")).thenReturn(List.of(new Application(), new Application()));

        assertEquals(2, applicationService.getByStatus("SUBMITTED").size());
    }

    @Test
    void getAll_delegatesToRepository() {
        when(applicationRepository.findAll()).thenReturn(List.of(new Application()));

        assertEquals(1, applicationService.getAll().size());
    }

    // ---------- updateStatus() ----------

    @Test
    void updateStatus_notFound_throwsException() {
        when(applicationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> applicationService.updateStatus(9L, "REVIEWED"));
    }

    @Test
    void updateStatus_success() {
        Application app = new Application();
        app.setId(5L);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.updateStatus(5L, "REVIEWED");

        assertEquals("REVIEWED", result.getStatus());
    }

    // ---------- setOutcome() ----------

    @Test
    void setOutcome_meetsRequirementsOnly_noEventPublished() {
        Application app = new Application();
        app.setId(5L);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationOutcomeRequest req = new ApplicationOutcomeRequest();
        req.setMeetsRequirements(true);
        req.setRequirementsReason("Strong CV");

        Application result = applicationService.setOutcome(5L, req);

        assertTrue(result.getMeetsRequirements());
        assertEquals("Strong CV", result.getRequirementsReason());
        assertNotNull(result.getReviewedAt());
        assertNull(result.getOutcome());
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void setOutcome_accepted_setsStatusHiredAndPublishesEvent() {
        JobPosting posting = new JobPosting();
        posting.setId(1L);
        posting.setTitle("Backend Developer");

        Application app = new Application();
        app.setId(5L);
        app.setJobPosting(posting);
        app.setCandidateEmail("jane@example.com");
        app.setCandidateName("Jane Candidate");

        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationOutcomeRequest req = new ApplicationOutcomeRequest();
        req.setOutcome("ACCEPTED");
        req.setOutcomeReason("Great interview");

        Application result = applicationService.setOutcome(5L, req);

        assertEquals("HIRED", result.getStatus());
        assertEquals("ACCEPTED", result.getOutcome());
        assertNotNull(result.getDecidedAt());

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("application.outcome.changed"),
                any(ApplicationOutcomeChangedEvent.class));
    }

    @Test
    void setOutcome_rejected_setsStatusRejectedAndPublishesEvent() {
        Application app = new Application();
        app.setId(6L);

        when(applicationRepository.findById(6L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationOutcomeRequest req = new ApplicationOutcomeRequest();
        req.setOutcome("REJECTED");
        req.setOutcomeReason("Not enough experience");

        Application result = applicationService.setOutcome(6L, req);

        assertEquals("REJECTED", result.getStatus());
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("application.outcome.changed"),
                any(ApplicationOutcomeChangedEvent.class));
    }

    @Test
    void setOutcome_notFound_throwsException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationOutcomeRequest req = new ApplicationOutcomeRequest();
        assertThrows(RuntimeException.class, () -> applicationService.setOutcome(99L, req));
    }
}