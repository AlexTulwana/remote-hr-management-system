package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HearingSummaryTest {

    private Hearing hearing() {
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        employee.setSalary(50000.0);

        Employee hrEmployee = new Employee();
        hrEmployee.setFullName("Alex Admin");
        hrEmployee.setSalary(90000.0);
        hrEmployee.setBankingDetails("test-bank-details");
        User conductor = new User();
        conductor.setUsername("admintest2");
        conductor.setEmployee(hrEmployee);

        Hearing hearing = new Hearing();
        hearing.setId(1L);
        hearing.setEmployee(employee);
        hearing.setConductedBy(conductor);
        hearing.setCaseType("Absenteeism");
        hearing.setDescription("Three unexplained absences");
        hearing.setHearingDateTime(LocalDateTime.of(2026, 10, 1, 10, 0));
        hearing.setMeetingLink("https://meet.example.com/abc");
        hearing.setStatus("COMPLETED");
        hearing.setOutcome("Written warning");
        hearing.setNotes("Internal HR notes");
        return hearing;
    }

    @Test
    void from_hidesNotesAndShowsConductorNameOnly() {
        HearingSummary summary = HearingSummary.from(hearing());

        assertThat(summary.getNotes()).isNull();
        assertThat(summary.getOutcome()).isEqualTo("Written warning");
        assertThat(summary.getConductedByName()).isEqualTo("Alex Admin");
        assertThat(summary.getEmployee().getFullName()).isEqualTo("Emma Employee");
        assertThat(summary.getMeetingLink()).isEqualTo("https://meet.example.com/abc");
    }

    @Test
    void forStaff_includesNotes() {
        assertThat(HearingSummary.forStaff(hearing()).getNotes()).isEqualTo("Internal HR notes");
    }

    @Test
    void from_conductorWithoutEmployee_usesUsername() {
        Hearing hearing = hearing();
        hearing.getConductedBy().setEmployee(null);

        assertThat(HearingSummary.from(hearing).getConductedByName()).isEqualTo("admintest2");
    }
}
