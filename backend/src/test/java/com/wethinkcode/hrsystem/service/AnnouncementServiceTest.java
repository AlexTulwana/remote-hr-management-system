package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.AnnouncementRequest;
import com.wethinkcode.hrsystem.model.Announcement;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.AnnouncementRepository;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CurrentUserService currentUserService;

    @TempDir
    Path tempDir;

    private AnnouncementService announcementService;

    private Branch branch1;
    private Branch branch2;
    private AnnouncementRequest request;

    @BeforeEach
    void setUp() {
        announcementService = new AnnouncementService(announcementRepository, userRepository,
                branchRepository, currentUserService, tempDir.toString());

        branch1 = new Branch();
        branch1.setId(1L);

        branch2 = new Branch();
        branch2.setId(2L);

        request = new AnnouncementRequest();
        request.setTitle("Office Closure");
        request.setContent("Office closed for a public holiday.");
        request.setCategory("Notice");
    }

    private User userWithRole(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    private Employee employeeWithBranch(Branch branch) {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setBranch(branch);
        return employee;
    }

    // ---- create() ----

    @Test
    void create_managerOwnBranch_succeeds() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchId(1L);
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = announcementService.create(request, null);

        assertEquals(branch1, result.getBranch());
        assertEquals(manager, result.getPostedBy());
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    void create_managerDifferentBranch_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchId(2L);

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_managerNoBranchIdProvided_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        // request.branchId left null - manager trying to go "global"

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_managerNotAssignedToBranch_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(null));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchId(1L);

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_managerNoEmployeeRecord_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(null);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchId(1L);

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_hrWithSpecificBranch_succeeds() {
        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        request.setBranchId(2L);
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch2));
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = announcementService.create(request, null);

        assertEquals(branch2, result.getBranch());
    }

    @Test
    void create_hrWithInvalidBranch_throwsRuntimeException() {
        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        request.setBranchId(99L);
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.create(request, null));
        assertEquals("Branch not found", ex.getMessage());
    }

    @Test
    void create_adminNoBranchId_postsGlobally() {
        User admin = userWithRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        // request.branchId left null - global announcement
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = announcementService.create(request, null);

        assertNull(result.getBranch());
        verify(branchRepository, never()).findById(any());
    }

    @Test
    void create_employeeRole_throwsAccessDenied() {
        User employee = userWithRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employee);

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_withPoster_savesFileAndSetsPath() throws Exception {
        User admin = userWithRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile poster = new MockMultipartFile(
                "poster", "notice.png", "image/png", "image-bytes".getBytes(StandardCharsets.UTF_8));

        Announcement result = announcementService.create(request, poster);

        assertNotNull(result.getPosterImagePath());
        assertTrue(Files.exists(Path.of(result.getPosterImagePath())));
    }

    // ---- getActive() / getAll() / getById() ----

    @Test
    void getActive_returnsActiveAnnouncements() {
        when(announcementRepository.findActive(any())).thenReturn(List.of(new Announcement()));

        List<Announcement> result = announcementService.getActive();

        assertEquals(1, result.size());
    }

    @Test
    void getAll_returnsAllAnnouncements() {
        when(announcementRepository.findAll()).thenReturn(List.of(new Announcement(), new Announcement()));

        List<Announcement> result = announcementService.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void getById_found_returnsAnnouncement() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        Announcement result = announcementService.getById(5L);

        assertEquals(announcement, result);
    }

    @Test
    void getById_notFound_throwsRuntimeException() {
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.getById(99L));
        assertEquals("Announcement not found", ex.getMessage());
    }

    // ---- delete() ----

    @Test
    void delete_managerOwnBranchAnnouncement_succeeds() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        announcement.setBranch(branch1);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        announcementService.delete(5L);

        verify(announcementRepository).deleteById(5L);
    }

    @Test
    void delete_managerOtherBranchAnnouncement_throwsAccessDenied() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        announcement.setBranch(branch2);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        assertThrows(AccessDeniedException.class, () -> announcementService.delete(5L));
        verify(announcementRepository, never()).deleteById(any());
    }

    @Test
    void delete_managerGlobalAnnouncement_throwsAccessDenied() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        announcement.setBranch(null);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        assertThrows(AccessDeniedException.class, () -> announcementService.delete(5L));
        verify(announcementRepository, never()).deleteById(any());
    }

    @Test
    void delete_hrDeletesGlobalAnnouncement_succeeds() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        announcement.setBranch(null);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);

        announcementService.delete(5L);

        verify(announcementRepository).deleteById(5L);
    }

    @Test
    void delete_employeeRole_throwsAccessDenied() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        User employee = userWithRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employee);

        assertThrows(AccessDeniedException.class, () -> announcementService.delete(5L));
        verify(announcementRepository, never()).deleteById(any());
    }

    @Test
    void delete_nonExistentId_throwsRuntimeExceptionBeforeAnyAuthCheck() {
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.delete(99L));
        assertEquals("Announcement not found", ex.getMessage());

        verify(announcementRepository, never()).deleteById(any());
        verifyNoInteractions(currentUserService);
    }

    // ---- getPoster() ----

    @Test
    void getPoster_fileExists_returnsResource() throws Exception {
        Path realFile = tempDir.resolve("poster.png");
        Files.writeString(realFile, "poster bytes");

        Announcement announcement = new Announcement();
        announcement.setId(5L);
        announcement.setPosterImagePath(realFile.toString());
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        Resource resource = announcementService.getPoster(5L);

        assertTrue(resource.exists());
    }

    @Test
    void getPoster_noPosterSet_throwsRuntimeException() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        announcement.setPosterImagePath(null);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.getPoster(5L));
        assertEquals("No poster image for this announcement", ex.getMessage());
    }

    @Test
    void getPoster_fileMissingOnDisk_throwsRuntimeException() {
        Announcement announcement = new Announcement();
        announcement.setId(5L);
        announcement.setPosterImagePath(tempDir.resolve("missing.png").toString());
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.getPoster(5L));
        assertEquals("Poster file not found on disk", ex.getMessage());
    }
}