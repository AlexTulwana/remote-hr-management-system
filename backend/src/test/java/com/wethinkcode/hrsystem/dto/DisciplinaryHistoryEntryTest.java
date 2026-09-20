package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.DisciplinaryCaseHistory;
import com.wethinkcode.hrsystem.model.DisciplinaryStage;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DisciplinaryHistoryEntryTest {

    private DisciplinaryCaseHistory history() {
        User actor = new User();
        actor.setId(2L);
        actor.setUsername("mgrtest1");

        DisciplinaryCaseHistory h = new DisciplinaryCaseHistory();
        h.setId(7L);
        h.setStage(DisciplinaryStage.VERBAL_WARNING);
        h.setComment("Late three days running");
        h.setActionedAt(LocalDateTime.of(2026, 9, 20, 9, 15));
        h.setActionedBy(actor);
        return h;
    }

    @Test
    void from_mapsFields() {
        DisciplinaryHistoryEntry entry = DisciplinaryHistoryEntry.from(history());

        assertThat(entry.getId()).isEqualTo(7L);
        assertThat(entry.getStage()).isEqualTo(DisciplinaryStage.VERBAL_WARNING);
        assertThat(entry.getComment()).isEqualTo("Late three days running");
        assertThat(entry.getActionedByUsername()).isEqualTo("mgrtest1");
        assertThat(entry.getLinkedHearingId()).isNull();
    }

    @Test
    void from_withLinkedHearing_includesHearingId() {
        Hearing hearing = new Hearing();
        hearing.setId(5L);
        DisciplinaryCaseHistory h = history();
        h.setLinkedHearing(hearing);

        assertThat(DisciplinaryHistoryEntry.from(h).getLinkedHearingId()).isEqualTo(5L);
    }
}
