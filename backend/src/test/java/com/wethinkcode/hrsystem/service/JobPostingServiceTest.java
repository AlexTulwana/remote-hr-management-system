package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmailTemplateUpdateRequest;
import com.wethinkcode.hrsystem.dto.JobPostingRequest;
import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {

    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private JobPostingService jobPostingService;

    private User poster;
    private JobPostingRequest request;

    @BeforeEach
    void setUp() {
        poster = new User();
        poster.setId(1L);
        poster.setUsername("hrtest2");

        request = new JobPostingRequest();
        request.setTitle("Backend Developer");
        request.setDescription("Build APIs");
        request.setRequirements("3+ years Java");
        request.setDepartment("IT");
        request.setStartDate(LocalDate.of(2026, 1, 1));
        request.setEndDate(LocalDate.of(2026, 2, 1));
        request.setMaxApplications(50);
        request.setPostedById(1L);
        request.setRequiredDocuments(List.of(DocumentType.CV, DocumentType.COVER_LETTER));
    }

    // ---------- create() ----------

    @Test
    void create_userNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jobPostingService.create(request));
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    void create_success_mapsAllFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(poster));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosting result = jobPostingService.create(request);

        assertEquals("Backend Developer", result.getTitle());
        assertEquals("Build APIs", result.getDescription());
        assertEquals("3+ years Java", result.getRequirements());
        assertEquals("IT", result.getDepartment());
        assertEquals(request.getStartDate(), result.getStartDate());
        assertEquals(request.getEndDate(), result.getEndDate());
        assertEquals(50, result.getMaxApplications());
        assertEquals(poster, result.getPostedBy());
        assertEquals(List.of(DocumentType.CV, DocumentType.COVER_LETTER), result.getRequiredDocuments());
    }

    @Test
    void create_nullRequiredDocuments_leavesDefaultEmptyList() {
        request.setRequiredDocuments(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(poster));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosting result = jobPostingService.create(request);

        assertNotNull(result.getRequiredDocuments());
        assertTrue(result.getRequiredDocuments().isEmpty());
    }

    // ---------- update() ----------

    @Test
    void update_notFound_throwsException() {
        when(jobPostingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jobPostingService.update(99L, request));
    }

    @Test
    void update_success_overwritesFields() {
        JobPosting existing = new JobPosting();
        existing.setId(5L);
        existing.setTitle("Old Title");
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosting result = jobPostingService.update(5L, request);

        assertEquals("Backend Developer", result.getTitle());
        assertEquals("IT", result.getDepartment());
    }

    @Test
    void update_nullRequiredDocuments_leavesExistingUnchanged() {
        JobPosting existing = new JobPosting();
        existing.setId(5L);
        existing.setRequiredDocuments(List.of(DocumentType.ID_COPY));
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        request.setRequiredDocuments(null);
        JobPosting result = jobPostingService.update(5L, request);

        assertEquals(List.of(DocumentType.ID_COPY), result.getRequiredDocuments());
    }

    // ---------- updateEmailTemplates() ----------

    @Test
    void updateEmailTemplates_notFound_throwsException() {
        when(jobPostingRepository.findById(99L)).thenReturn(Optional.empty());

        EmailTemplateUpdateRequest templateRequest = new EmailTemplateUpdateRequest();
        assertThrows(RuntimeException.class,
                () -> jobPostingService.updateEmailTemplates(99L, templateRequest));
    }

    @Test
    void updateEmailTemplates_onlySetFieldsAreUpdated() {
        JobPosting existing = new JobPosting();
        existing.setId(5L);
        existing.setRejectedEmailTemplate("old rejected");
        existing.setAcceptedEmailTemplate("old accepted");
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailTemplateUpdateRequest templateRequest = new EmailTemplateUpdateRequest();
        templateRequest.setInterviewInviteEmailTemplate("new invite");
        // rejected/accepted left null intentionally

        JobPosting result = jobPostingService.updateEmailTemplates(5L, templateRequest);

        assertEquals("old rejected", result.getRejectedEmailTemplate());
        assertEquals("new invite", result.getInterviewInviteEmailTemplate());
        assertEquals("old accepted", result.getAcceptedEmailTemplate());
    }

    @Test
    void updateEmailTemplates_allFieldsSet_allUpdated() {
        JobPosting existing = new JobPosting();
        existing.setId(5L);
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailTemplateUpdateRequest templateRequest = new EmailTemplateUpdateRequest();
        templateRequest.setRejectedEmailTemplate("rejected");
        templateRequest.setInterviewInviteEmailTemplate("invite");
        templateRequest.setAcceptedEmailTemplate("accepted");

        JobPosting result = jobPostingService.updateEmailTemplates(5L, templateRequest);

        assertEquals("rejected", result.getRejectedEmailTemplate());
        assertEquals("invite", result.getInterviewInviteEmailTemplate());
        assertEquals("accepted", result.getAcceptedEmailTemplate());
    }

    // ---------- getOpen() ----------

    @Test
    void getOpen_filtersToTodayInRange() {
        LocalDate today = LocalDate.now();

        JobPosting open = new JobPosting();
        open.setStartDate(today.minusDays(1));
        open.setEndDate(today.plusDays(1));

        JobPosting notYetOpen = new JobPosting();
        notYetOpen.setStartDate(today.plusDays(5));
        notYetOpen.setEndDate(today.plusDays(10));

        JobPosting closed = new JobPosting();
        closed.setStartDate(today.minusDays(10));
        closed.setEndDate(today.minusDays(1));

        when(jobPostingRepository.findAll()).thenReturn(List.of(open, notYetOpen, closed));

        List<JobPosting> result = jobPostingService.getOpen();

        assertEquals(1, result.size());
        assertSame(open, result.get(0));
    }

    @Test
    void getOpen_boundaryDatesInclusive() {
        LocalDate today = LocalDate.now();

        JobPosting startsToday = new JobPosting();
        startsToday.setStartDate(today);
        startsToday.setEndDate(today.plusDays(5));

        JobPosting endsToday = new JobPosting();
        endsToday.setStartDate(today.minusDays(5));
        endsToday.setEndDate(today);

        when(jobPostingRepository.findAll()).thenReturn(List.of(startsToday, endsToday));

        List<JobPosting> result = jobPostingService.getOpen();

        assertEquals(2, result.size());
    }

    // ---------- getAll() / getById() / delete() ----------

    @Test
    void getAll_delegatesToRepository() {
        when(jobPostingRepository.findAll()).thenReturn(List.of(new JobPosting(), new JobPosting()));

        assertEquals(2, jobPostingService.getAll().size());
    }

    @Test
    void getById_found_returnsPosting() {
        JobPosting posting = new JobPosting();
        posting.setId(7L);
        when(jobPostingRepository.findById(7L)).thenReturn(Optional.of(posting));

        assertEquals(posting, jobPostingService.getById(7L));
    }

    @Test
    void getById_notFound_throwsException() {
        when(jobPostingRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jobPostingService.getById(8L));
    }

    @Test
    void delete_delegatesToRepository() {
        jobPostingService.delete(5L);

        verify(jobPostingRepository).deleteById(5L);
    }
}