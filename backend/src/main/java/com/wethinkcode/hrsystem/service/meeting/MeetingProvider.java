package com.wethinkcode.hrsystem.service.meeting;

public interface MeetingProvider {

    MeetingResult createMeeting(MeetingDetails details);

    String getProviderName();
}