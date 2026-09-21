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
import java.util.Set;

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

    private Announcement announcementFor(Long id, Branch... branches) {
        Announcement announcement = new Announcement();
        announcement.setId(id);
        announcement.setBranches(Set.of(branches));
        return announcement;
    }

    // ---- create() ----

    @Test
    void create_managerOwnBranch_succeeds() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchIds(List.of(1L));
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = announcementService.create(request, null);

        assertEquals(Set.of(branch1), result.getBranches());
        assertEquals(manager, result.getPostedBy());
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    void create_managerDifferentBranch_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchIds(List.of(2L));

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_managerSeveralBranches_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchIds(List.of(1L, 2L));

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_managerNoBranchIdsProvided_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        // request.branchIds left null - manager trying to post to everyone

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_managerNotAssignedToBranch_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(null));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchIds(List.of(1L));

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_managerNoEmployeeRecord_throwsAccessDenied() {
        User manager = userWithRole("MANAGER");
        manager.setEmployee(null);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        request.setBranchIds(List.of(1L));

        assertThrows(AccessDeniedException.class, () -> announcementService.create(request, null));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_hrWithSpecificBranch_succeeds() {
        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        request.setBranchIds(List.of(2L));
        when(branchRepository.findAllById(List.of(2L))).thenReturn(List.of(branch2));
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = announcementService.create(request, null);

        assertEquals(Set.of(branch2), result.getBranches());
    }

    @Test
    void create_hrWithSeveralBranches_succeeds() {
        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        request.setBranchIds(List.of(1L, 2L));
        when(branchRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(branch1, branch2));
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = announcementService.create(request, null);

        assertEquals(Set.of(branch1, branch2), result.getBranches());
    }

    @Test
    void create_hrWithInvalidBranch_throwsRuntimeException() {
        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        request.setBranchIds(List.of(99L));
        when(branchRepository.findAllById(List.of(99L))).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.create(request, null));
        assertEquals("Branch not found", ex.getMessage());
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_hrWithOneInvalidBranchAmongValid_throwsRuntimeException() {
        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        request.setBranchIds(List.of(1L, 99L));
        when(branchRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(branch1));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.create(request, null));
        assertEquals("Branch not found", ex.getMessage());
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void create_adminNoBranchIds_postsToEveryone() {
        User admin = userWithRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        // request.branchIds left null - announcement for everyone
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = announcementService.create(request, null);

        assertTrue(result.getBranches().isEmpty());
        verify(branchRepository, never()).findAllById(any());
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

    // ---- getActiveForViewer() ----

    @Test
    void getActiveForViewer_hr_seesEverything() {
        when(currentUserService.isHrOrAdmin()).thenReturn(true);
        when(announcementRepository.findActive(any())).thenReturn(List.of(
                announcementFor(1L), announcementFor(2L, branch1), announcementFor(3L, branch2)));

        assertEquals(3, announcementService.getActiveForViewer().size());
    }

    @Test
    void getActiveForViewer_employee_seesEveryoneAndOwnBranchOnly() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);
        when(announcementRepository.findActive(any())).thenReturn(List.of(
                announcementFor(1L),
                announcementFor(2L, branch1),
                announcementFor(3L, branch2),
                announcementFor(4L, branch1, branch2)));

        List<Long> visibleIds = announcementService.getActiveForViewer().stream()
                .map(Announcement::getId).toList();

        assertEquals(List.of(1L, 2L, 4L), visibleIds);
    }

    @Test
    void getActiveForViewer_employeeWithNoBranch_seesOnlyEveryone() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(null);
        when(announcementRepository.findActive(any())).thenReturn(List.of(
                announcementFor(1L), announcementFor(2L, branch1)));

        List<Long> visibleIds = announcementService.getActiveForViewer().stream()
                .map(Announcement::getId).toList();

        assertEquals(List.of(1L), visibleIds);
    }

    // ---- getAll() / getById() / getByIdForViewer() ----

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

    @Test
    void getByIdForViewer_ownBranch_returnsAnnouncement() {
        Announcement announcement = announcementFor(5L, branch1);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);

        assertEquals(announcement, announcementService.getByIdForViewer(5L));
    }

    @Test
    void getByIdForViewer_otherBranch_isTreatedAsNotFound() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L, branch2)));
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.getByIdForViewer(5L));
        assertEquals("Announcement not found", ex.getMessage());
    }

    @Test
    void getByIdForViewer_hr_seesAnyBranch() {
        Announcement announcement = announcementFor(5L, branch2);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcement));
        when(currentUserService.isHrOrAdmin()).thenReturn(true);

        assertEquals(announcement, announcementService.getByIdForViewer(5L));
    }

    // ---- delete() ----

    @Test
    void delete_managerOwnBranchOnlyAnnouncement_succeeds() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L, branch1)));

        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        announcementService.delete(5L);

        verify(announcementRepository).deleteById(5L);
    }

    @Test
    void delete_managerOtherBranchAnnouncement_throwsAccessDenied() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L, branch2)));

        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        assertThrows(AccessDeniedException.class, () -> announcementService.delete(5L));
        verify(announcementRepository, never()).deleteById(any());
    }

    @Test
    void delete_managerEveryoneAnnouncement_throwsAccessDenied() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L)));

        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        assertThrows(AccessDeniedException.class, () -> announcementService.delete(5L));
        verify(announcementRepository, never()).deleteById(any());
    }

    @Test
    void delete_managerMultiBranchAnnouncementIncludingOwn_throwsAccessDenied() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L, branch1, branch2)));

        User manager = userWithRole("MANAGER");
        manager.setEmployee(employeeWithBranch(branch1));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        assertThrows(AccessDeniedException.class, () -> announcementService.delete(5L));
        verify(announcementRepository, never()).deleteById(any());
    }

    @Test
    void delete_hrDeletesEveryoneAnnouncement_succeeds() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L)));

        User hr = userWithRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);

        announcementService.delete(5L);

        verify(announcementRepository).deleteById(5L);
    }

    @Test
    void delete_employeeRole_throwsAccessDenied() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L)));

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

    @Test
    void getPoster_announcementForOtherBranch_isTreatedAsNotFound() {
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(announcementFor(5L, branch2)));
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> announcementService.getPoster(5L));
        assertEquals("Announcement not found", ex.getMessage());
    }
}
