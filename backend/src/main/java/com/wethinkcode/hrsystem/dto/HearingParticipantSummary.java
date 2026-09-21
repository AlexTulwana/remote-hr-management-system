package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.HearingParticipant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HearingParticipantSummary {
    private Long id;
    private Long userId;
    private String personName;
    private String role;
    private boolean attended;

    public static HearingParticipantSummary from(HearingParticipant p) {
        Long userId = null;
        String personName = null;
        if (p.getPerson() != null) {
            userId = p.getPerson().getId();
            personName = p.getPerson().getEmployee() != null
                    && p.getPerson().getEmployee().getFullName() != null
                    ? p.getPerson().getEmployee().getFullName()
                    : p.getPerson().getUsername();
        }
        return new HearingParticipantSummary(p.getId(), userId, personName, p.getRole(), p.isAttended());
    }
}
