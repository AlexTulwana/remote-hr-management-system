package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.HearingRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.HearingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HearingService {

    private final HearingRepository hearingRepository;
    private final EmployeeRepository employeeRepository;

    public HearingService(HearingRepository hearingRepository, EmployeeRepository employeeRepository) {
        this.hearingRepository = hearingRepository;
        this.employeeRepository = employeeRepository;
    }

    public Hearing schedule(HearingRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Hearing hearing = new Hearing();
        hearing.setEmployee(employee);
        hearing.setCaseType(request.getCaseType());
        hearing.setDescription(request.getDescription());
        hearing.setHearingDateTime(request.getHearingDateTime());
        hearing.setMeetingLink(request.getMeetingLink());
        hearing.setStatus("SCHEDULED");

        Hearing saved = hearingRepository.save(hearing);

        // Notification placeholder — Module 12 will build this out properly.
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