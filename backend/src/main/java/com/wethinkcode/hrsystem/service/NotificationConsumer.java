package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.HearingScheduledEvent;
import com.wethinkcode.hrsystem.dto.InterviewScheduledEvent;
import com.wethinkcode.hrsystem.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
public class NotificationConsumer {

    private final JavaMailSender mailSender;

    public NotificationConsumer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(e.candidateEmail());
            message.setSubject("Interview Scheduled - " + (e.jobTitle() != null ? e.jobTitle() : ""));
            message.setText("Dear " + e.candidateName() + ",\n\n"
                    + "Your interview has been scheduled for " + e.scheduledAt() + ".\n"
                    + (e.meetingLink() != null ? "Join link: " + e.meetingLink() + "\n" : "Location: " + e.location() + "\n")
                    + "\nRegards,\nHR Team");
            mailSender.send(message);
            System.out.println("CONSUMER: interview email sent successfully to " + e.candidateEmail());
        } catch (Exception ex) {
            System.out.println("CONSUMER: FAILED to send interview email - " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
