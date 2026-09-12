package com.wethinkcode.hrsystem.service.meeting;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.util.DateTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.UUID;

@Component
public class GoogleMeetProvider implements MeetingProvider {

    private final ObjectProvider<Calendar> calendarServiceProvider;

    public GoogleMeetProvider(ObjectProvider<Calendar> calendarServiceProvider) {
        this.calendarServiceProvider = calendarServiceProvider;
    }

    @Override
    public MeetingResult createMeeting(MeetingDetails details) {
        try {
            Event event = new Event()
                    .setSummary("Disciplinary Hearing: " + details.getCaseType())
                    .setDescription("Hearing for " + details.getEmployeeName());

            DateTime startDateTime = new DateTime(
                    details.getDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            DateTime endDateTime = new DateTime(
                    details.getDateTime().plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            event.setStart(new EventDateTime().setDateTime(startDateTime));
            event.setEnd(new EventDateTime().setDateTime(endDateTime));

            ConferenceData conferenceData = new ConferenceData()
                    .setCreateRequest(new CreateConferenceRequest()
                            .setRequestId(UUID.randomUUID().toString())
                            .setConferenceSolutionKey(new ConferenceSolutionKey().setType("hangoutsMeet")));
            event.setConferenceData(conferenceData);

            Calendar calendarService = calendarServiceProvider.getObject();
            Event createdEvent = calendarService.events()
                    .insert("primary", event)
                    .setConferenceDataVersion(1)
                    .execute();

            String meetLink = createdEvent.getHangoutLink();

            return new MeetingResult(meetLink, createdEvent.getId(), true, null);

        } catch (Exception e) {
            return new MeetingResult(null, null, false, e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Google Meet";
    }
}