package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.HearingRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.HearingRepository;
import com.wethinkcode.hrsystem.service.meeting.MeetingDetails;
import com.wethinkcode.hrsystem.service.meeting.MeetingProvider;
import com.wethinkcode.hrsystem.service.meeting.MeetingResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HearingService {

    private final HearingRepository hearingRepository;
    private final EmployeeRepository employeeRepository;

    // Optional: if a MeetingProvider bean exists, it gets injected here.
    // If none is configured, this stays null and we fall back to the manually pasted link.
    private final Optional<MeetingProvider> meetingProvider;

    public HearingService(HearingRepository hearingRepository,
                          EmployeeRepository employeeRepository,
                          Optional<MeetingProvider> meetingProvider) {
        this.hearingRepository = hearingRepository;
        this.employeeRepository = employeeRepository;
        this.meetingProvider = meetingProvider;
    }

    public Hearing schedule(HearingRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Hearing hearing = new Hearing();
        hearing.setEmployee(employee);
        hearing.setCaseType(request.getCaseType());
        hearing.setDescription(request.getDescription());
        hearing.setHearingDateTime(request.getHearingDateTime());
        hearing.setStatus("SCHEDULED");

        String meetingLink = request.getMeetingLink();

        // Stage 8c will populate meetingProvider with a real bean (Zoom/Google Meet).
        // Until then, this block is dormant and we just use the manually pasted link.
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
        return hearingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hearing not found"));
    }

    public List<Hearing> getByEmployee(Long employeeId) {
        return hearingRepository.findByEmployeeId(employeeId);
    }

    public List<Hearing> getAll() {
        return hearingRepository.findAll();
    }
}