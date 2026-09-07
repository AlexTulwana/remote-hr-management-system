package com.wethinkcode.hrsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wethinkcode.hrsystem.dto.CalendarItem;
import com.wethinkcode.hrsystem.model.CalendarEvent;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.CalendarEventRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.CalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalendarController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CalendarService calendarService;

    @MockitoBean
    private CalendarEventRepository calendarEventRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---- getCalendar: any authenticated user ----

    @Test
    void getCalendar_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/calendar")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void getCalendar_employeeRole_isAllowed() throws Exception {
        User emma = new User();
        emma.setUsername("emptest1");
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(emma));
        when(calendarService.getCalendar(any(), any(), any(), any())).thenReturn(List.of(new CalendarItem()));

        mockMvc.perform(get("/api/calendar")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk());
    }

    // ---- createEvent (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createEvent_employeeRole_isForbidden() throws Exception {
        CalendarEvent event = new CalendarEvent();

        mockMvc.perform(post("/api/calendar/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void createEvent_hrRole_isAllowed() throws Exception {
        User hr = new User();
        hr.setUsername("hrtest2");
        hr.setRole("HR");
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hr));

        CalendarEvent saved = new CalendarEvent();
        saved.setId(1L);
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenReturn(saved);

        CalendarEvent event = new CalendarEvent();

        mockMvc.perform(post("/api/calendar/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated());
    }

    // ---- updateEvent (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEvent_employeeRole_isForbidden() throws Exception {
        CalendarEvent event = new CalendarEvent();

        mockMvc.perform(put("/api/calendar/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void updateEvent_hrRole_isAllowed() throws Exception {
        User hr = new User();
        hr.setUsername("hrtest2");
        hr.setRole("HR");
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hr));

        CalendarEvent existing = new CalendarEvent();
        existing.setId(1L);
        when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenReturn(existing);

        CalendarEvent event = new CalendarEvent();

        mockMvc.perform(put("/api/calendar/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());
    }

    // ---- deleteEvent (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteEvent_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/calendar/events/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void deleteEvent_hrRole_isAllowed() throws Exception {
        User hr = new User();
        hr.setUsername("hrtest2");
        hr.setRole("HR");
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hr));

        CalendarEvent existing = new CalendarEvent();
        existing.setId(1L);
        when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/api/calendar/events/1"))
                .andExpect(status().isNoContent());
    }
}