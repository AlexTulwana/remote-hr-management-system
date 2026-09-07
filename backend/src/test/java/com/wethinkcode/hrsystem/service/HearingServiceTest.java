package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.HearingParticipantRequest;
import com.wethinkcode.hrsystem.dto.HearingRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.model.HearingParticipant;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.HearingParticipantRepository;
import com.wethinkcode.hrsystem.repository.HearingRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.service.meeting.MeetingDetails;
import com.wethinkcode.hrsystem.service.meeting.MeetingProvider;
import com.wethinkcode.hrsystem.service.meeting.MeetingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HearingServiceTest {

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private HearingParticipantRepository hearingParticipantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private MeetingProvider meetingProvider;

    private HearingService hearingService;

    private Employee employee;
    private User conductedBy;
    private Hearing existingHearing;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        employee.setEmail("emma.employee@example.com");

        conductedBy = new User();
        conductedBy.setUsername("hrtest2");
        conductedBy.setRole("HR");

        existingHearing = new Hearing();
        existingHearing.setId(1L);
        existingHearing.setEmployee(employee);
        existingHearing.setStatus("SCHEDULED");
    }

    private HearingService serviceWithProvider(Optional<MeetingProvider> provider) {
        return new HearingService(hearingRepository, employeeRepository, provider,
                hearingParticipantRepository, userRepository, currentUserService, rabbitTemplate);
    }

    // --- schedule ---

    @Test
    void schedule_withProvidedMeetingLink_doesNotCallProvider() {
        hearingService = serviceWithProvider(Optional.of(meetingProvider));

        HearingRequest request = new HearingRequest();
        request.setEmployeeId(4L);
        request.setCaseType("Absenteeism");
        request.setHearingDateTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        request.setMeetingLink("https://manual-link.example.com");

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(conductedBy));
        when(hearingRepository.save(any(Hearing.class))).thenAnswer(inv -> inv.getArgument(0));

        Hearing result = hearingService.schedule(request, "hrtest2");

        assertThat(result.getMeetingLink()).isEqualTo("https://manual-link.example.com");
        verify(meetingProvider, never()).createMeeting(any());
    }

    @Test
    void schedule_noLinkWithProvider_createsMeetingViaProvider() {
        hearingService = serviceWithProvider(Optional.of(meetingProvider));

        HearingRequest request = new HearingRequest();
        request.setEmployeeId(4L);
        request.setCaseType("Absenteeism");
        request.setHearingDateTime(LocalDateTime.of(2026, 9, 1, 10, 0));

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(conductedBy));
        when(meetingProvider.createMeeting(any(MeetingDetails.class)))
                .thenReturn(new MeetingResult("https://generated-link.example.com", "meeting-id-123", true, null));
        when(hearingRepository.save(any(Hearing.class))).thenAnswer(inv -> inv.getArgument(0));

        Hearing result = hearingService.schedule(request, "hrtest2");

        assertThat(result.getMeetingLink()).isEqualTo("https://generated-link.example.com");
    }

    @Test
    void schedule_providerFails_throwsException() {
        hearingService = serviceWithProvider(Optional.of(meetingProvider));

        HearingRequest request = new HearingRequest();
        request.setEmployeeId(4L);
        request.setCaseType("Absenteeism");
        request.setHearingDateTime(LocalDateTime.of(2026, 9, 1, 10, 0));

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(conductedBy));
        when(meetingProvider.getProviderName()).thenReturn("GoogleMeet");
        when(meetingProvider.createMeeting(any(MeetingDetails.class)))
                .thenReturn(new MeetingResult(null, null, false, "API quota exceeded"));

        assertThatThrownBy(() -> hearingService.schedule(request, "hrtest2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create meeting via GoogleMeet");
    }

    @Test
    void schedule_noProviderNoLink_savesWithNullMeetingLink() {
        hearingService = serviceWithProvider(Optional.empty());

        HearingRequest request = new HearingRequest();
        request.setEmployeeId(4L);
        request.setCaseType("Absenteeism");
        request.setHearingDateTime(LocalDateTime.of(2026, 9, 1, 10, 0));

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(conductedBy));
        when(hearingRepository.save(any(Hearing.class))).thenAnswer(inv -> inv.getArgument(0));

        Hearing result = hearingService.schedule(request, "hrtest2");

        assertThat(result.getMeetingLink()).isNull();
    }

    @Test
    void schedule_unknownEmployee_throwsException() {
        hearingService = serviceWithProvider(Optional.empty());

        HearingRequest request = new HearingRequest();
        request.setEmployeeId(99L);

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hearingService.schedule(request, "hrtest2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Employee not found");
    }

    @Test
    void schedule_publishesHearingScheduledEvent() {
        hearingService = serviceWithProvider(Optional.empty());

        HearingRequest request = new HearingRequest();
        request.setEmployeeId(4L);
        request.setCaseType("Absenteeism");
        request.setHearingDateTime(LocalDateTime.of(2026, 9, 1, 10, 0));

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(conductedBy));
        when(hearingRepository.save(any(Hearing.class))).thenAnswer(inv -> inv.getArgument(0));

        hearingService.schedule(request, "hrtest2");

        verify(rabbitTemplate).convertAndSend(eq("hr.events"), eq("hearing.scheduled"), any(Object.class));
    }

    // --- addParticipant ---

    @Test
    void addParticipant_success() {
        hearingService = serviceWithProvider(Optional.empty());

        HearingParticipantRequest request = new HearingParticipantRequest();
        request.setUserId(10L);
        request.setRole("WITNESS");

        User person = new User();
        person.setId(10L);

        when(currentUserService.getCurrentUser()).thenReturn(conductedBy);
        when(hearingRepository.findById(1L)).thenReturn(Optional.of(existingHearing));
        when(userRepository.findById(10L)).thenReturn(Optional.of(person));
        when(hearingParticipantRepository.save(any(HearingParticipant.class))).thenAnswer(inv -> inv.getArgument(0));

        HearingParticipant result = hearingService.addParticipant(1L, request);

        assertThat(result.getHearing()).isEqualTo(existingHearing);
        assertThat(result.getPerson()).isEqualTo(person);
        assertThat(result.getRole()).isEqualTo("WITNESS");
    }

    @Test
    void addParticipant_unknownUser_throwsException() {
        hearingService = serviceWithProvider(Optional.empty());

        HearingParticipantRequest request = new HearingParticipantRequest();
        request.setUserId(99L);

        when(currentUserService.getCurrentUser()).thenReturn(conductedBy);
        when(hearingRepository.findById(1L)).thenReturn(Optional.of(existingHearing));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hearingService.addParticipant(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // --- getParticipants ---

    @Test
    void getParticipants_returnsList() {
        hearingService = serviceWithProvider(Optional.empty());

        HearingParticipant participant = new HearingParticipant();
        when(hearingParticipantRepository.findByHearingId(1L)).thenReturn(List.of(participant));

        assertThat(hearingService.getParticipants(1L)).containsExactly(participant);
    }

    // --- updateOutcome / cancel ---

    @Test
    void updateOutcome_setsFieldsAndCompletesStatus() {
        hearingService = serviceWithProvider(Optional.empty());

        when(currentUserService.getCurrentUser()).thenReturn(conductedBy);
        when(hearingRepository.findById(1L)).thenReturn(Optional.of(existingHearing));
        when(hearingRepository.save(any(Hearing.class))).thenAnswer(inv -> inv.getArgument(0));

        Hearing result = hearingService.updateOutcome(1L, "Verbal warning issued", "Employee cooperative");

        assertThat(result.getOutcome()).isEqualTo("Verbal warning issued");
        assertThat(result.getNotes()).isEqualTo("Employee cooperative");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void cancel_setsStatusCancelled() {
        hearingService = serviceWithProvider(Optional.empty());

        when(currentUserService.getCurrentUser()).thenReturn(conductedBy);
        when(hearingRepository.findById(1L)).thenReturn(Optional.of(existingHearing));
        when(hearingRepository.save(any(Hearing.class))).thenAnswer(inv -> inv.getArgument(0));

        Hearing result = hearingService.cancel(1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }

    // --- getById (authorization) ---

    @Test
    void getById_asHr_bypassesSelfCheck() {
        hearingService = serviceWithProvider(Optional.empty());

        when(hearingRepository.findById(1L)).thenReturn(Optional.of(existingHearing));
        when(currentUserService.getCurrentUser()).thenReturn(conductedBy);

        Hearing result = hearingService.getById(1L);

        assertThat(result).isEqualTo(existingHearing);
        verify(currentUserService, never()).isSelf(anyLong());
    }

    @Test
    void getById_asSelf_allowed() {
        hearingService = serviceWithProvider(Optional.empty());

        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");

        when(hearingRepository.findById(1L)).thenReturn(Optional.of(existingHearing));
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(4L)).thenReturn(true);

        Hearing result = hearingService.getById(1L);

        assertThat(result).isEqualTo(existingHearing);
    }

    @Test
    void getById_asOtherEmployee_throwsAccessDenied() {
        hearingService = serviceWithProvider(Optional.empty());

        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");

        when(hearingRepository.findById(1L)).thenReturn(Optional.of(existingHearing));
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(4L)).thenReturn(false);

        assertThatThrownBy(() -> hearingService.getById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not authorized to view this hearing");
    }

    @Test
    void getById_notFound_throwsException() {
        hearingService = serviceWithProvider(Optional.empty());

        when(hearingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hearingService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Hearing not found");
    }

    // --- getByEmployee / getAll ---

    @Test
    void getByEmployee_returnsHearingsForEmployee() {
        hearingService = serviceWithProvider(Optional.empty());

        when(currentUserService.getCurrentUser()).thenReturn(conductedBy);

        when(hearingRepository.findByEmployeeId(4L)).thenReturn(List.of(existingHearing));

        assertThat(hearingService.getByEmployee(4L)).containsExactly(existingHearing);
    }

    @Test
    void getAll_returnsAllHearings() {
        hearingService = serviceWithProvider(Optional.empty());

        when(hearingRepository.findAll()).thenReturn(List.of(existingHearing));

        assertThat(hearingService.getAll()).containsExactly(existingHearing);
    }
}