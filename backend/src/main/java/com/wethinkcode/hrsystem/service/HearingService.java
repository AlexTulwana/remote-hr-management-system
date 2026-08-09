package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.HearingRequest;
import com.wethinkcode.hrsystem.dto.HearingParticipantRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.model.HearingParticipant;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.HearingParticipantRepository;
import com.wethinkcode.hrsystem.repository.HearingRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.service.meeting.MeetingDetails;
import com.wethinkcode.hrsystem.service.meeting.MeetingProvider;
import com.wethinkcode.hrsystem.service.meeting.MeetingResult;
import org.springframework.stereotype.Service;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;



import java.util.List;
import java.util.Optional;

@Service
public class HearingService {

    private final HearingRepository hearingRepository;
    private final EmployeeRepository employeeRepository;
    private final Optional<MeetingProvider> meetingProvider;
    private final HearingParticipantRepository hearingParticipantRepository;
    private final UserRepository userRepository;
    private final com.wethinkcode.hrsystem.security.CurrentUserService currentUserService;

    public HearingService(HearingRepository hearingRepository,
                          EmployeeRepository employeeRepository,
                          Optional<MeetingProvider> meetingProvider,
                          HearingParticipantRepository hearingParticipantRepository,
                          UserRepository userRepository,
                          CurrentUserService currentUserService) {
        this.hearingRepository = hearingRepository;
        this.employeeRepository = employeeRepository;
        this.meetingProvider = meetingProvider;
        this.hearingParticipantRepository = hearingParticipantRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public Hearing schedule(HearingRequest request, String username) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        User conductedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Hearing hearing = new Hearing();
        hearing.setEmployee(employee);
        hearing.setConductedBy(conductedBy);
        hearing.setCaseType(request.getCaseType());
        hearing.setDescription(request.getDescription());
        hearing.setHearingDateTime(request.getHearingDateTime());
        hearing.setStatus("SCHEDULED");

        String meetingLink = request.getMeetingLink();

        if (meetingProvider.isPresent() && (meetingLink == null || meetingLink.isBlank())) {
            MeetingDetails details = new MeetingDetails(
                    employee.getFullName(), request.getCaseType(), request.getHearingDateTime());
            MeetingResult result = meetingProvider.get().createMeeting(details);

            if (result.isSuccess()) {
                meetingLink = result.getJoinUrl();
            } else {
                throw new RuntimeException("Failed to create meeting via "
                        + meetingProvider.get().getProviderName() + ": " + result.getErrorMessage());
            }
        }

        hearing.setMeetingLink(meetingLink);

        Hearing saved = hearingRepository.save(hearing);

        System.out.println("NOTIFICATION: Hearing scheduled for employee "
                + employee.getFullName() + " on " + hearing.getHearingDateTime()
                + ". Join link: " + hearing.getMeetingLink());

        return saved;
    }

    public HearingParticipant addParticipant(Long hearingId, HearingParticipantRequest request) {
        Hearing hearing = getById(hearingId);
        User person = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        HearingParticipant participant = new HearingParticipant();
        participant.setHearing(hearing);
        participant.setPerson(person);
        participant.setRole(request.getRole());

        return hearingParticipantRepository.save(participant);
    }

    public List<HearingParticipant> getParticipants(Long hearingId) {
        return hearingParticipantRepository.findByHearingId(hearingId);
    }

    public Hearing updateOutcome(Long hearingId, String outcome, String notes) {
        Hearing hearing = getById(hearingId);
        hearing.setOutcome(outcome);
        hearing.setNotes(notes);
        hearing.setStatus("COMPLETED");
        return hearingRepository.save(hearing);
    }

    public Hearing cancel(Long hearingId) {
        Hearing hearing = getById(hearingId);
        hearing.setStatus("CANCELLED");
        return hearingRepository.save(hearing);
    }

    public Hearing getById(Long id) {
        Hearing hearing = hearingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hearing not found"));

        String role = currentUserService.getCurrentUser().getRole();
        if (!role.equals("HR") && !role.equals("ADMIN")) {
            if (!currentUserService.isSelf(hearing.getEmployee().getId())) {
                throw new AccessDeniedException("You are not authorized to view this hearing");
            }
        }

        return hearing;
    }

    public List<Hearing> getByEmployee(Long employeeId) {
        return hearingRepository.findByEmployeeId(employeeId);
    }

    public List<Hearing> getAll() {
        return hearingRepository.findAll();
    }
}