package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Attendance;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private JwtUtil jwtUtil;


    @Test
    void clockIn_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in/1"))
                .andExpect(status().isForbidden());   // was isUnauthorized()
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void clockIn_allowedByService_returnsOk() throws Exception {
        Attendance attendance = new Attendance();
        attendance.setId(1L);
        when(attendanceService.clockIn(anyLong())).thenReturn(attendance);

        mockMvc.perform(post("/api/attendance/clock-in/4"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void clockIn_deniedByService_returnsForbidden() throws Exception {
        when(attendanceService.clockIn(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to act on this employee's attendance"));

        mockMvc.perform(post("/api/attendance/clock-in/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void clockOut_allowedByService_returnsOk() throws Exception {
        Attendance attendance = new Attendance();
        attendance.setId(2L);
        when(attendanceService.clockOut(anyLong())).thenReturn(attendance);

        mockMvc.perform(patch("/api/attendance/clock-out/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void clockOut_deniedByService_returnsForbidden() throws Exception {
        when(attendanceService.clockOut(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to act on this employee's attendance"));

        mockMvc.perform(patch("/api/attendance/clock-out/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_allowedByService_returnsOk() throws Exception {
        when(attendanceService.getByEmployee(anyLong())).thenReturn(List.of(new Attendance()));

        mockMvc.perform(get("/api/attendance/employee/4"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_deniedByService_returnsForbidden() throws Exception {
        when(attendanceService.getByEmployee(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to act on this employee's attendance"));

        mockMvc.perform(get("/api/attendance/employee/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_ownBranch_returnsOk() throws Exception {
        when(attendanceService.getByBranch(anyLong())).thenReturn(List.of(new Attendance()));

        mockMvc.perform(get("/api/attendance/branch/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_otherBranch_returnsForbidden() throws Exception {
        when(attendanceService.getByBranch(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to view this branch's attendance"));

        mockMvc.perform(get("/api/attendance/branch/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAll_hrRole_isAllowed() throws Exception {
        when(attendanceService.getAll()).thenReturn(List.of(new Attendance(), new Attendance()));

        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_adminRole_isAllowed() throws Exception {
        when(attendanceService.getAll()).thenReturn(List.of(new Attendance()));

        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isOk());
    }
}