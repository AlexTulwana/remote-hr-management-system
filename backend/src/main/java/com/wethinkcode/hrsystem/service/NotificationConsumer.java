package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ApplicationOutcomeChangedEvent;
import com.wethinkcode.hrsystem.dto.HearingScheduledEvent;
import com.wethinkcode.hrsystem.dto.InterviewScheduledEvent;
import com.wethinkcode.hrsystem.config.RabbitMQConfig;
import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
public class NotificationConsumer {

    private final JavaMailSender mailSender;
    private final JobPostingRepository jobPostingRepository;

    public NotificationConsumer(JavaMailSender mailSender, JobPostingRepository jobPostingRepository) {
        this.mailSender = mailSender;
        this.jobPostingRepository = jobPostingRepository;
    }

    @RabbitHandler
    public void handleHearing(HearingScheduledEvent e) {
        System.out.println("CONSUMER: handling HearingScheduledEvent for " + e.employeeName());
        try {
            if (e.employeeEmail() == null || e.employeeEmail().isBlank()) {
                System.out.println("Skipping hearing email - no address for employee " + e.employeeName());
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(e.employeeEmail());
            message.setSubject("Hearing Scheduled");
            message.setText("Dear " + e.employeeName() + ",\n\n"
                    + "A hearing has been scheduled for you on " + e.scheduledAt() + ".\n"
                    + (e.meetingLink() != null ? "Join link: " + e.meetingLink() + "\n" : "")
                    + "Conducted by: " + e.conductedByName() + "\n\n"
                    + "Regards,\nHR Team");
            mailSender.send(message);
            System.out.println("CONSUMER: hearing email sent successfully to " + e.employeeEmail());
        } catch (Exception ex) {
            System.out.println("CONSUMER: FAILED to send hearing email - " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @RabbitHandler
    public void handleInterview(InterviewScheduledEvent e) {
        System.out.println("CONSUMER: handling InterviewScheduledEvent for " + e.candidateName());
        try {
            if (e.candidateEmail() == null || e.candidateEmail().isBlank()) {
                System.out.println("Skipping interview email - no address for candidate " + e.candidateName());
                return;
            }

            String template = lookupTemplate(e.jobPostingId(), JobPosting::getInterviewInviteEmailTemplate);

            String body;
            if (template != null && !template.isBlank()) {
                body = applyPlaceholders(template, e.candidateName(), e.jobTitle());
            } else {
                body = "Dear " + e.candidateName() + ",\n\n"
                        + "Your interview has been scheduled for " + formatDateTime(e.scheduledAt()) + ".\n"
                        + (e.meetingLink() != null ? "Join link: " + e.meetingLink() + "\n" : "Location: " + e.location() + "\n")
                        + "\nRegards,\nHR Team";
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(e.candidateEmail());
            message.setSubject("Interview Scheduled - " + (e.jobTitle() != null ? e.jobTitle() : ""));
            message.setText(body);
            mailSender.send(message);
            System.out.println("CONSUMER: interview email sent successfully to " + e.candidateEmail());
        } catch (Exception ex) {
            System.out.println("CONSUMER: FAILED to send interview email - " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @RabbitHandler
    public void handleApplicationOutcomeChanged(ApplicationOutcomeChangedEvent e) {
        System.out.println("CONSUMER: handling ApplicationOutcomeChangedEvent for " + e.candidateName());
        try {
            if (e.candidateEmail() == null || e.candidateEmail().isBlank()) {
                System.out.println("Skipping outcome email - no address for candidate " + e.candidateName());
                return;
            }

            boolean accepted = "ACCEPTED".equals(e.outcome());

            String template = lookupTemplate(e.jobPostingId(),
                    accepted ? JobPosting::getAcceptedEmailTemplate : JobPosting::getRejectedEmailTemplate);

            String body;
            if (template != null && !template.isBlank()) {
                body = applyPlaceholders(template, e.candidateName(), e.jobTitle());
            } else if (accepted) {
                body = "Dear " + e.candidateName() + ",\n\n"
                        + "Congratulations! We are pleased to inform you that your application"
                        + (e.jobTitle() != null ? " for " + e.jobTitle() : "") + " has been accepted.\n\n"
                        + "Regards,\nHR Team";
            } else {
                body = "Dear " + e.candidateName() + ",\n\n"
                        + "Thank you for applying" + (e.jobTitle() != null ? " for " + e.jobTitle() : "") + ". "
                        + "After careful consideration, we have decided not to proceed with your application at this time.\n\n"
                        + "Regards,\nHR Team";
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(e.candidateEmail());
            message.setSubject("Application Update" + (e.jobTitle() != null ? " - " + e.jobTitle() : ""));
            message.setText(body);
            mailSender.send(message);
            System.out.println("CONSUMER: outcome email sent successfully to " + e.candidateEmail());
        } catch (Exception ex) {
            System.out.println("CONSUMER: FAILED to send outcome email - " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"));
    }

    private String lookupTemplate(Long jobPostingId, java.util.function.Function<JobPosting, String> templateGetter) {
        if (jobPostingId == null) {
            return null;
        }
        Optional<JobPosting> posting = jobPostingRepository.findById(jobPostingId);
        return posting.map(templateGetter).orElse(null);
    }

    private String applyPlaceholders(String template, String candidateName, String jobTitle) {
        return template
                .replace("{candidateName}", candidateName != null ? candidateName : "")
                .replace("{jobTitle}", jobTitle != null ? jobTitle : "");
    }
}