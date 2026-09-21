package com.wethinkcode.hrsystem.controller;
import org.springframework.context.annotation.Import;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.model.HearingParticipant;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.service.HearingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.wethinkcode.hrsystem.security.JwtUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HearingController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class HearingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private HearingService hearingService;

    private static final String HEARING_JSON =
            "{\"employeeId\":4,\"caseType\":\"Absenteeism\",\"hearingDateTime\":\"2026-10-01T10:00:00\"}";

    // ---------- schedule ----------

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void schedule_asHr_returns200() throws Exception {
        Hearing hearing = new Hearing();
        hearing.setId(1L);

        when(hearingService.schedule(any(), eq("hrtest2"))).thenReturn(hearing);

        mockMvc.perform(post("/api/hearings")
                        .contentType("application/json")
                        .content(HEARING_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void schedule_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/hearings")
                        .contentType("application/json")
                        .content(HEARING_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void schedule_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/hearings")
                        .contentType("application/json")
                        .content(HEARING_JSON))
                .andExpect(status().isForbidden());
    }

    // ---------- updateOutcome ----------

    @Test
    @WithMockUser(roles = "HR")
    void updateOutcome_asHr_returns200() throws Exception {
        Hearing hearing = new Hearing();
        hearing.setId(1L);
        hearing.setStatus("COMPLETED");

        when(hearingService.updateOutcome(eq(1L), anyString(), anyString())).thenReturn(hearing);

        mockMvc.perform(patch("/api/hearings/1/outcome")
                        .contentType("application/json")
                        .content("{\"outcome\":\"Verbal warning\",\"notes\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateOutcome_asEmployee_returns403() throws Exception {
        mockMvc.perform(patch("/api/hearings/1/outcome")
                        .contentType("application/json")
                        .content("{\"outcome\":\"x\",\"notes\":\"y\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- cancel ----------

    @Test
    @WithMockUser(roles = "HR")
    void cancel_asHr_returns200() throws Exception {
        Hearing hearing = new Hearing();
        hearing.setId(1L);
        hearing.setStatus("CANCELLED");

        when(hearingService.cancel(1L)).thenReturn(hearing);

        mockMvc.perform(patch("/api/hearings/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void cancel_asEmployee_returns403() throws Exception {
        mockMvc.perform(patch("/api/hearings/1/cancel"))
                .andExpect(status().isForbidden());
    }

    // ---------- getById ----------

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_success_returns200() throws Exception {
        Hearing hearing = new Hearing();
        hearing.setId(1L);

        when(hearingService.getById(1L)).thenReturn(hearing);

        mockMvc.perform(get("/api/hearings/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_serviceThrowsAccessDenied_returns403() throws Exception {
        when(hearingService.getById(1L))
                .thenThrow(new AccessDeniedException("You are not authorized to view this hearing"));

        mockMvc.perform(get("/api/hearings/1"))
                .andExpect(status().isForbidden());
    }

    // ---------- getByEmployee ----------

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_success_returns200() throws Exception {
        when(hearingService.getByEmployee(4L)).thenReturn(List.of(new Hearing()));

        mockMvc.perform(get("/api/hearings/employee/4"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_serviceThrowsAccessDenied_returns403() throws Exception {
        when(hearingService.getByEmployee(3L))
                .thenThrow(new AccessDeniedException("You are not authorized to view these hearings"));

        mockMvc.perform(get("/api/hearings/employee/3"))
                .andExpect(status().isForbidden());
    }

    // ---------- getAll ----------

    @Test
    @WithMockUser(roles = "HR")
    void getAll_asHr_returns200() throws Exception {
        when(hearingService.getAll()).thenReturn(List.of(new Hearing()));

        mockMvc.perform(get("/api/hearings"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/hearings"))
                .andExpect(status().isForbidden());
    }

    // ---------- participants ----------

    @Test
    @WithMockUser(roles = "HR")
    void addParticipant_asHr_returns200() throws Exception {
        HearingParticipant participant = new HearingParticipant();
        participant.setId(1L);

        when(hearingService.addParticipant(eq(1L), any())).thenReturn(participant);

        mockMvc.perform(post("/api/hearings/1/participants")
                        .contentType("application/json")
                        .content("{\"userId\":10,\"role\":\"WITNESS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addParticipant_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/hearings/1/participants")
                        .contentType("application/json")
                        .content("{\"userId\":10,\"role\":\"WITNESS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getParticipants_asHr_returns200() throws Exception {
        when(hearingService.getParticipants(1L)).thenReturn(List.of(new HearingParticipant()));

        mockMvc.perform(get("/api/hearings/1/participants"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getParticipants_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/hearings/1/participants"))
                .andExpect(status().isForbidden());
    }

    // ---------- visibility of notes and staff details ----------

    private Hearing completedHearing() {
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        employee.setSalary(50000.0);

        Employee hrEmployee = new Employee();
        hrEmployee.setFullName("Alex Admin");
        hrEmployee.setSalary(90000.0);
        hrEmployee.setBankingDetails("test-bank-details");
        hrEmployee.setIdNumber("test-id-number");
        User conductor = new User();
        conductor.setUsername("admintest2");
        conductor.setEmployee(hrEmployee);

        Hearing hearing = new Hearing();
        hearing.setId(1L);
        hearing.setEmployee(employee);
        hearing.setConductedBy(conductor);
        hearing.setStatus("COMPLETED");
        hearing.setOutcome("Written warning");
        hearing.setNotes("Internal HR notes");
        return hearing;
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_employee_seesOutcomeButNotNotesOrConductorRecord() throws Exception {
        when(hearingService.getById(1L)).thenReturn(completedHearing());

        mockMvc.perform(get("/api/hearings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("Written warning"))
                .andExpect(jsonPath("$.notes").isEmpty())
                .andExpect(jsonPath("$.conductedByName").value("Alex Admin"))
                .andExpect(jsonPath("$.conductedBy").doesNotExist())
                .andExpect(jsonPath("$.employee.salary").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getById_hr_seesNotes() throws Exception {
        when(hearingService.getById(1L)).thenReturn(completedHearing());

        mockMvc.perform(get("/api/hearings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Internal HR notes"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_employee_doesNotSeeNotes() throws Exception {
        when(hearingService.getByEmployee(4L)).thenReturn(List.of(completedHearing()));

        mockMvc.perform(get("/api/hearings/employee/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notes").isEmpty());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getParticipants_returnsSummaryWithoutUserOrHearingRecords() throws Exception {
        Employee employee = new Employee();
        employee.setFullName("Sam Manager");
        employee.setSalary(60000.0);
        User person = new User();
        person.setId(10L);
        person.setEmployee(employee);
        HearingParticipant participant = new HearingParticipant();
        participant.setId(1L);
        participant.setPerson(person);
        participant.setHearing(completedHearing());
        participant.setRole("WITNESS");

        when(hearingService.getParticipants(1L)).thenReturn(List.of(participant));

        mockMvc.perform(get("/api/hearings/1/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personName").value("Sam Manager"))
                .andExpect(jsonPath("$[0].person").doesNotExist())
                .andExpect(jsonPath("$[0].hearing").doesNotExist());
    }
}
