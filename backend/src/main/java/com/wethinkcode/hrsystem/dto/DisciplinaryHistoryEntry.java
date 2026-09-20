package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.DisciplinaryCaseHistory;
import com.wethinkcode.hrsystem.model.DisciplinaryStage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DisciplinaryHistoryEntry {
    private Long id;
    private DisciplinaryStage stage;
    private String comment;
    private LocalDateTime actionedAt;
    private String actionedByUsername;
    private Long linkedHearingId;

    public static DisciplinaryHistoryEntry from(DisciplinaryCaseHistory h) {
        return new DisciplinaryHistoryEntry(
                h.getId(),
                h.getStage(),
                h.getComment(),
                h.getActionedAt(),
                h.getActionedBy() != null ? h.getActionedBy().getUsername() : null,
                h.getLinkedHearing() != null ? h.getLinkedHearing().getId() : null
        );
    }
}
