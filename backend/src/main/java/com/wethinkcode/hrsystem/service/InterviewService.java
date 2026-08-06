package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.InterviewRequest;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.Interview;
import com.wethinkcode.hrsystem.repository.ApplicationRepository;
import com.wethinkcode.hrsystem.repository.InterviewRepository;
import com.wethinkcode.hrsystem.service.meeting.MeetingDetails;
import com.wethinkcode.hrsystem.service.meeting.MeetingProvider;
import com.wethinkcode.hrsystem.service.meeting.MeetingResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InterviewService {

    private static final List<String> VALID_STATUSES = List.of("SCHEDULED", "COMPLETED", "CANCELLED");

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final Optional<MeetingProvider> meetingProvider;

    public InterviewService(InterviewRepository interviewRepository,
                            ApplicationRepository applicationRepository,
                            Optional<MeetingProvider> meetingProvider) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.meetingProvider = meetingProvider;
    }

    public Interview schedule(InterviewRequest request) {
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new RuntimeException("Application not found"));

        boolean alreadyScheduled = interviewRepository.findByApplicationId(application.getId())
                .stream().anyMatch(i -> "SCHEDULED".equals(i.getStatus()));
        if (alreadyScheduled) {
            throw new RuntimeException("An interview is already scheduled for this application");
        }

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setType(request.getType());
        interview.setInterviewDateTime(request.getInterviewDateTime());
        interview.setStatus("SCHEDULED");

        if ("ONLINE".equals(request.getType())) {
            String meetingLink = request.getMeetingLink();

            if (meetingLink == null || meetingLink.isBlank()) {
                if (meetingProvider.isEmpty()) {
                    throw new RuntimeException("No meeting link provided and no meeting provider configured");
                }
                MeetingDetails details = new MeetingDetails(
                        application.getCandidateName(), "Interview", request.getInterviewDateTime());
                MeetingResult result = meetingProvider.get().createMeeting(details);

                if (result.isSuccess()) {
                    meetingLink = result.getJoinUrl();
                } else {
                    throw new RuntimeException("Failed to create interview meeting via "
                            + meetingProvider.get().getProviderName() + ": " + result.getErrorMessage());
                }
            }

            interview.setMeetingLink(meetingLink);
        } else {
            interview.setLocation(request.getLocation());
        }

        application.setStatus("INTERVIEW_SCHEDULED");
        applicationRepository.save(application);

        Interview saved = interviewRepository.save(interview);

        System.out.println("NOTIFICATION: Interview scheduled for candidate "
                + application.getCandidateName() + " on " + interview.getInterviewDateTime()
                + (interview.getMeetingLink() != null ? ". Join link: " + interview.getMeetingLink() : ". Location: " + interview.getLocation()));

        return saved;
    }

    public Interview updateStatus(Long interviewId, String status, String notes) {
        if (!VALID_STATUSES.contains(status)) {
            throw new RuntimeException("Invalid status: " + status);
        }
        Interview interview = getById(interviewId);
        interview.setStatus(status);
        interview.setNotes(notes);
        return interviewRepository.save(interview);
    }

    public Interview getById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
    }

    public List<Interview> getByApplication(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }

    public List<Interview> getAll() {
        return interviewRepository.findAll();
    }
}