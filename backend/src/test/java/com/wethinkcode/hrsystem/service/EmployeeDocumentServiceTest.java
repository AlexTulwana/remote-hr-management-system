package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.EmployeeDocumentRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeDocumentServiceTest {

    @Mock
    private EmployeeDocumentRepository documentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @TempDir
    Path tempDir;

    private EmployeeDocumentService documentService;

    private User hrUser;
    private User employeeUser;
    private User managerUser;
    private Employee employee;
    private Employee managerEmployee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentService = new EmployeeDocumentService(
                documentRepository, employeeRepository, userRepository,
                tempDir.toString() + "/");

        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Emma Employee");

        managerEmployee = new Employee();
        managerEmployee.setId(3L);
        managerEmployee.setFullName("Mark Manager");

        hrUser = new User();
        hrUser.setId(10L);
        hrUser.setUsername("hrtest2");
        hrUser.setRole("HR");

        employeeUser = new User();
        employeeUser.setId(20L);
        employeeUser.setUsername("emptest1");
        employeeUser.setRole("EMPLOYEE");
        employeeUser.setEmployee(employee);

        managerUser = new User();
        managerUser.setId(30L);
        managerUser.setUsername("mgrtest1");
        managerUser.setRole("MANAGER");
        managerUser.setEmployee(managerEmployee);
    }

    // ---------- upload() - HR/Admin (unrestricted) ----------

    @Test
    void upload_employeeNotFound_throwsException() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.upload(1L, file, DocumentType.CV, "desc", "hrtest2"));
        assertTrue(ex.getMessage().contains("Employee not found"));
    }

    @Test
    void upload_emptyFile_throwsException() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0]);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.upload(1L, emptyFile, DocumentType.CV, "desc", "hrtest2"));
        assertEquals("File is empty", ex.getMessage());
    }

    @Test
    void upload_fileTooLarge_throwsException() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        byte[] tooBig = new byte[6 * 1024 * 1024];
        MockMultipartFile bigFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", tooBig);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.upload(1L, bigFile, DocumentType.CV, "desc", "hrtest2"));
        assertEquals("File exceeds 5MB limit", ex.getMessage());
    }

    @Test
    void upload_disallowedExtension_throwsException() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "content".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.upload(1L, exeFile, DocumentType.CV, "desc", "hrtest2"));
        assertTrue(ex.getMessage().contains("File type not allowed"));
    }

    @Test
    void upload_validPdf_success() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        EmployeeDocument result = documentService.upload(1L, file, DocumentType.CV, "my cv", "hrtest2");

        assertEquals(employee, result.getEmployee());
        assertEquals(DocumentType.CV, result.getDocumentType());
        assertEquals("my cv", result.getDescription());
        assertEquals(hrUser, result.getUploadedBy());
        assertNotNull(result.getUploadedAt());
        assertTrue(result.getFileName().endsWith(".pdf"));
        assertTrue(java.nio.file.Files.exists(tempDir.resolve(result.getFileName())));
    }

    @Test
    void upload_sanitizesFilename_usesUuidNaming() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "weird name!@#.PDF", "application/pdf", "content".getBytes());

        EmployeeDocument result = documentService.upload(1L, file, DocumentType.OTHER, null, "hrtest2");

        // saved filename is a UUID + lowercase extension, not the original name
        assertNotEquals("weird name!@#.PDF", result.getFileName());
        assertTrue(result.getFileName().endsWith(".pdf"));
    }

    @Test
    void upload_hrUser_restrictedType_forOtherEmployee_success() {
        // HR is not subject to the self-upload type restriction
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "content".getBytes());

        EmployeeDocument result = documentService.upload(1L, file, DocumentType.CONTRACT, "signed contract", "hrtest2");

        assertEquals(DocumentType.CONTRACT, result.getDocumentType());
    }

    // ---------- upload() - self-upload (employee/manager) ----------

    @Test
    void upload_employeeOwnRecord_permittedType_success() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "sick-note.pdf", "application/pdf", "content".getBytes());

        EmployeeDocument result = documentService.upload(1L, file, DocumentType.MEDICAL_CERTIFICATE,
                "sick leave proof", "emptest1");

        assertEquals(DocumentType.MEDICAL_CERTIFICATE, result.getDocumentType());
        assertEquals(employeeUser, result.getUploadedBy());
    }

    @Test
    void upload_employeeOwnRecord_taxDocument_success() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "tax.pdf", "application/pdf", "content".getBytes());

        EmployeeDocument result = documentService.upload(1L, file, DocumentType.TAX_DOCUMENT, null, "emptest1");

        assertEquals(DocumentType.TAX_DOCUMENT, result.getDocumentType());
    }

    @Test
    void upload_managerOwnRecord_permittedType_success() {
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(managerUser));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(managerEmployee));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "qualification.pdf", "application/pdf", "content".getBytes());

        EmployeeDocument result = documentService.upload(3L, file, DocumentType.QUALIFICATION, null, "mgrtest1");

        assertEquals(DocumentType.QUALIFICATION, result.getDocumentType());
    }

    @Test
    void upload_employeeOwnRecord_restrictedType_throwsAccessDenied() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));

        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "content".getBytes());

        assertThrows(AccessDeniedException.class,
                () -> documentService.upload(1L, file, DocumentType.CONTRACT, "desc", "emptest1"));
        verifyNoInteractions(employeeRepository, documentRepository);
    }

    @Test
    void upload_employeeOwnRecord_disciplinaryType_throwsAccessDenied() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));

        MockMultipartFile file = new MockMultipartFile("file", "warning.pdf", "application/pdf", "content".getBytes());

        assertThrows(AccessDeniedException.class,
                () -> documentService.upload(1L, file, DocumentType.DISCIPLINARY, "desc", "emptest1"));
    }

    @Test
    void upload_employeeOtherEmployeeRecord_throwsAccessDenied() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        // employeeUser's own record is id 1L, attempting to upload for id 2L
        assertThrows(AccessDeniedException.class,
                () -> documentService.upload(2L, file, DocumentType.CV, "desc", "emptest1"));
        verifyNoInteractions(employeeRepository, documentRepository);
    }

    @Test
    void upload_userWithNoLinkedEmployee_throwsAccessDenied() {
        User orphan = new User();
        orphan.setUsername("orphan");
        orphan.setRole("EMPLOYEE");
        orphan.setEmployee(null);
        when(userRepository.findByUsername("orphan")).thenReturn(Optional.of(orphan));

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        assertThrows(AccessDeniedException.class,
                () -> documentService.upload(1L, file, DocumentType.CV, "desc", "orphan"));
    }

    // ---------- list() ----------

    @Test
    void list_hrUser_canViewAnyEmployee() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(documentRepository.findByEmployeeIdAndDeletedFalse(1L)).thenReturn(List.of(new EmployeeDocument()));

        List<EmployeeDocument> result = documentService.list(1L, "hrtest2");

        assertEquals(1, result.size());
    }

    @Test
    void list_ownEmployee_canViewOwnDocuments() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));
        when(documentRepository.findByEmployeeIdAndDeletedFalse(1L)).thenReturn(List.of(new EmployeeDocument()));

        List<EmployeeDocument> result = documentService.list(1L, "emptest1");

        assertEquals(1, result.size());
    }

    @Test
    void list_otherEmployee_throwsAccessDenied() {
        Employee otherEmployee = new Employee();
        otherEmployee.setId(2L);
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setRole("EMPLOYEE");
        otherUser.setEmployee(otherEmployee);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () -> documentService.list(1L, "other"));
        verifyNoInteractions(documentRepository);
    }

    @Test
    void list_employeeWithNoLinkedRecord_throwsAccessDenied() {
        User noEmployeeUser = new User();
        noEmployeeUser.setUsername("orphan");
        noEmployeeUser.setRole("EMPLOYEE");
        noEmployeeUser.setEmployee(null);

        when(userRepository.findByUsername("orphan")).thenReturn(Optional.of(noEmployeeUser));

        assertThrows(AccessDeniedException.class, () -> documentService.list(1L, "orphan"));
    }

    // ---------- get() ----------

    @Test
    void get_notFound_throwsException() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(documentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> documentService.get(5L, "hrtest2"));
    }

    @Test
    void get_deletedDocument_throwsAccessDeniedEvenForHr() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployee(employee);
        doc.setDeleted(true);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        assertThrows(AccessDeniedException.class, () -> documentService.get(5L, "hrtest2"));
    }

    @Test
    void get_ownDocument_success() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployee(employee);
        doc.setDeleted(false);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        EmployeeDocument result = documentService.get(5L, "emptest1");

        assertEquals(doc, result);
    }

    @Test
    void get_otherEmployeeDocument_throwsAccessDenied() {
        Employee otherEmployee = new Employee();
        otherEmployee.setId(2L);
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setRole("EMPLOYEE");
        otherUser.setEmployee(otherEmployee);
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployee(employee); // belongs to employee id 1, not otherEmployee's id 2
        doc.setDeleted(false);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        assertThrows(AccessDeniedException.class, () -> documentService.get(5L, "other"));
    }

    // ---------- softDelete() ----------

    @Test
    void softDelete_nonHrUser_throwsAccessDenied() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));

        assertThrows(AccessDeniedException.class, () -> documentService.softDelete(5L, "emptest1"));
        verifyNoInteractions(documentRepository);
    }

    @Test
    void softDelete_notFound_throwsException() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(documentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> documentService.softDelete(5L, "hrtest2"));
    }

    @Test
    void softDelete_success_marksDeletedFields() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));

        EmployeeDocument doc = new EmployeeDocument();
        doc.setDeleted(false);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        documentService.softDelete(5L, "hrtest2");

        assertTrue(doc.isDeleted());
        assertNotNull(doc.getDeletedAt());
        assertEquals(hrUser, doc.getDeletedBy());
        verify(documentRepository).save(doc);
    }
}
