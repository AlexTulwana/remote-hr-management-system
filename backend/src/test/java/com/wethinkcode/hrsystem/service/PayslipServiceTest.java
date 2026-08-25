package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Payslip;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.PayslipRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayslipServiceTest {

    @Mock
    private PayslipRepository payslipRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @TempDir
    Path tempDir;

    private PayslipService payslipService;
    private Employee employee;

    @BeforeEach
    void setUp() {
        payslipService = new PayslipService(payslipRepository, employeeRepository,
                currentUserService, tempDir.toString());

        employee = new Employee();
        employee.setId(1L);
    }

    // ---- upload() ----

    @Test
    void upload_savesFileAndPersistsPayslip() throws IOException {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(payslipRepository.save(any(Payslip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "july-payslip.pdf", "application/pdf",
                "dummy content".getBytes(StandardCharsets.UTF_8));

        Payslip result = payslipService.upload(1L, "2026-07", file);

        assertNotNull(result);
        assertEquals(employee, result.getEmployee());
        assertEquals("2026-07", result.getPayPeriod());
        assertEquals(LocalDate.now(), result.getUploadedDate());
        assertNotNull(result.getFilePath());

        Path savedFile = Path.of(result.getFilePath());
        assertTrue(Files.exists(savedFile), "uploaded file should exist on disk");
        assertTrue(savedFile.getFileName().toString().endsWith("_july-payslip.pdf"));
        assertEquals("dummy content", Files.readString(savedFile));

        verify(payslipRepository).save(any(Payslip.class));
    }

    @Test
    void upload_employeeNotFound_throwsRuntimeException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "payslip.pdf", "application/pdf", "content".getBytes(StandardCharsets.UTF_8));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payslipService.upload(1L, "2026-07", file));
        assertEquals("Employee not found", ex.getMessage());

        verify(payslipRepository, never()).save(any());
    }

    // ---- getByEmployee() ----

    @Test
    void getByEmployee_asHR_returnsPayslipsWithoutSelfCheck() {
        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(payslipRepository.findByEmployeeId(1L)).thenReturn(List.of(new Payslip()));

        List<Payslip> result = payslipService.getByEmployee(1L);

        assertEquals(1, result.size());
        verify(currentUserService, never()).isSelf(any());
    }

    @Test
    void getByEmployee_asEmployeeViewingSelf_returnsPayslips() {
        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(1L)).thenReturn(true);
        when(payslipRepository.findByEmployeeId(1L)).thenReturn(List.of(new Payslip()));

        List<Payslip> result = payslipService.getByEmployee(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getByEmployee_asEmployeeViewingOther_throwsAccessDenied() {
        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> payslipService.getByEmployee(1L));

        verify(payslipRepository, never()).findByEmployeeId(any());
    }

    // ---- getById() ----

    @Test
    void getById_asAdmin_returnsPayslip() {
        Payslip payslip = new Payslip();
        payslip.setId(5L);
        payslip.setEmployee(employee);
        when(payslipRepository.findById(5L)).thenReturn(Optional.of(payslip));

        User adminUser = new User();
        adminUser.setRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        Payslip result = payslipService.getById(5L);

        assertEquals(payslip, result);
        verify(currentUserService, never()).isSelf(any());
    }

    @Test
    void getById_asEmployeeViewingSelf_returnsPayslip() {
        Payslip payslip = new Payslip();
        payslip.setId(5L);
        payslip.setEmployee(employee);
        when(payslipRepository.findById(5L)).thenReturn(Optional.of(payslip));

        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(1L)).thenReturn(true);

        Payslip result = payslipService.getById(5L);

        assertEquals(payslip, result);
    }

    @Test
    void getById_asEmployeeViewingOther_throwsAccessDenied() {
        Payslip payslip = new Payslip();
        payslip.setId(5L);
        payslip.setEmployee(employee);
        when(payslipRepository.findById(5L)).thenReturn(Optional.of(payslip));

        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> payslipService.getById(5L));
    }

    @Test
    void getById_payslipNotFound_throwsRuntimeException() {
        when(payslipRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payslipService.getById(99L));
        assertEquals("Payslip not found", ex.getMessage());
    }

    // ---- downloadFile() ----

    @Test
    void downloadFile_fileExists_returnsResource() throws IOException {
        Path realFile = tempDir.resolve("existing-payslip.pdf");
        Files.writeString(realFile, "payslip bytes");

        Payslip payslip = new Payslip();
        payslip.setId(7L);
        payslip.setEmployee(employee);
        payslip.setFilePath(realFile.toString());
        when(payslipRepository.findById(7L)).thenReturn(Optional.of(payslip));

        User adminUser = new User();
        adminUser.setRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        Resource resource = payslipService.downloadFile(7L);

        assertTrue(resource.exists());
    }

    @Test
    void downloadFile_fileMissing_throwsRuntimeException() {
        Payslip payslip = new Payslip();
        payslip.setId(8L);
        payslip.setEmployee(employee);
        payslip.setFilePath(tempDir.resolve("does-not-exist.pdf").toString());
        when(payslipRepository.findById(8L)).thenReturn(Optional.of(payslip));

        User adminUser = new User();
        adminUser.setRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payslipService.downloadFile(8L));
        assertTrue(ex.getMessage().startsWith("File not found"));
    }

    @Test
    void downloadFile_unauthorizedEmployee_throwsAccessDenied() {
        Payslip payslip = new Payslip();
        payslip.setId(9L);
        payslip.setEmployee(employee);
        payslip.setFilePath(tempDir.resolve("irrelevant.pdf").toString());
        when(payslipRepository.findById(9L)).thenReturn(Optional.of(payslip));

        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> payslipService.downloadFile(9L));
    }
}