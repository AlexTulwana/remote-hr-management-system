package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.HearingParticipant;
import com.wethinkcode.hrsystem.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HearingParticipantSummaryTest {

    @Test
    void from_mapsNameRoleAndAttendance() {
        Employee employee = new Employee();
        employee.setFullName("Sam Manager");
        employee.setSalary(60000.0);
        User person = new User();
        person.setId(10L);
        person.setUsername("mgrtest1");
        person.setEmployee(employee);

        HearingParticipant participant = new HearingParticipant();
        participant.setId(1L);
        participant.setPerson(person);
        participant.setRole("WITNESS");
        participant.setAttended(true);

        HearingParticipantSummary summary = HearingParticipantSummary.from(participant);

        assertThat(summary.getPersonName()).isEqualTo("Sam Manager");
        assertThat(summary.getUserId()).isEqualTo(10L);
        assertThat(summary.getRole()).isEqualTo("WITNESS");
        assertThat(summary.isAttended()).isTrue();
    }

    @Test
    void from_personWithoutEmployee_usesUsername() {
        User person = new User();
        person.setUsername("mgrtest1");
        HearingParticipant participant = new HearingParticipant();
        participant.setPerson(person);

        assertThat(HearingParticipantSummary.from(participant).getPersonName()).isEqualTo("mgrtest1");
    }

    @Test
    void from_noPerson_isNullSafe() {
        HearingParticipantSummary summary = HearingParticipantSummary.from(new HearingParticipant());

        assertThat(summary.getPersonName()).isNull();
        assertThat(summary.getUserId()).isNull();
    }
}
