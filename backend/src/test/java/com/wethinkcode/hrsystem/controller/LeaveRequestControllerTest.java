

import com.wethinkcode.hrsystem.controller.LeaveRequestController;
import com.wethinkcode.hrsystem.dto.LeaveRequestDto;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.service.LeaveRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveRequestController.class)
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    // ---------- submit ----------

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submit_success_returns200() throws Exception {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(1L);
        leave.setStatus("PENDING");

        when(leaveRequestService.submit(eq(4L), any(LeaveRequestDto.class), isNull())).thenReturn(leave);

        mockMvc.perform(multipart("/api/leave/4")
                        .param("leaveType", "ANNUAL")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-05")
                        .param("reason", "Vacation"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submit_serviceThrowsAccessDenied_returns403() throws Exception {
        when(leaveRequestService.submit(eq(3L), any(LeaveRequestDto.class), isNull()))
                .thenThrow(new AccessDeniedException("You are not authorized to perform this action for this employee"));

        mockMvc.perform(multipart("/api/leave/3")
                        .param("leaveType", "ANNUAL")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-05")
                        .param("reason", "Should be blocked"))
                .andExpect(status().isForbidden());
    }

    @Test
    void submit_unauthenticated_returns403() throws Exception {
        mockMvc.perform(multipart("/api/leave/4")
                        .param("leaveType", "ANNUAL")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-05")
                        .param("reason", "Vacation"))
                .andExpect(status().isForbidden());
    }

    // ---------- approve / reject ----------

    @Test
    @WithMockUser(roles = "MANAGER")
    void approve_asManager_returns200() throws Exception {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(1L);
        leave.setStatus("APPROVED");

        when(leaveRequestService.approve(1L)).thenReturn(leave);

        mockMvc.perform(patch("/api/leave/1/approve"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void approve_asEmployee_returns403() throws Exception {
        mockMvc.perform(patch("/api/leave/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void approve_managerDifferentBranch_serviceThrows_returns403() throws Exception {
        when(leaveRequestService.approve(1L))
                .thenThrow(new AccessDeniedException("Managers can only action leave requests for their own branch"));

        mockMvc.perform(patch("/api/leave/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void reject_asHr_returns200() throws Exception {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(1L);
        leave.setStatus("REJECTED");

        when(leaveRequestService.reject(eq(1L), anyString())).thenReturn(leave);

        mockMvc.perform(patch("/api/leave/1/reject")
                        .contentType("application/json")
                        .content("{\"reason\":\"Insufficient balance\"}"))
                .andExpect(status().isOk());
    }

    // ---------- getByEmployee / getByBranch ----------

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_success_returns200() throws Exception {
        when(leaveRequestService.getByEmployee(4L)).thenReturn(List.of(new LeaveRequest()));

        mockMvc.perform(get("/api/leave/employee/4"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_serviceThrowsAccessDenied_returns403() throws Exception {
        when(leaveRequestService.getByEmployee(3L))
                .thenThrow(new AccessDeniedException("You are not authorized to view these leave requests"));

        mockMvc.perform(get("/api/leave/employee/3"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_success_returns200() throws Exception {
        when(leaveRequestService.getByBranch(1L)).thenReturn(List.of(new LeaveRequest()));

        mockMvc.perform(get("/api/leave/branch/1"))
                .andExpect(status().isOk());
    }

    // ---------- getAll ----------

    @Test
    @WithMockUser(roles = "HR")
    void getAll_asHr_returns200() throws Exception {
        when(leaveRequestService.getAll()).thenReturn(List.of(new LeaveRequest()));

        mockMvc.perform(get("/api/leave"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/leave"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/leave"))
                .andExpect(status().isForbidden());
    }
}