package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.DisciplinaryCaseRequest;
import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisciplinaryCaseServiceTest {

    @Mock
    private DisciplinaryCaseRepository caseRepository;
    @Mock
    private DisciplinaryCaseHistoryRepository historyRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EscalationRepository escalationRepository;
    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private DisciplinaryCaseService disciplinaryCaseService;

    private Branch branch;
    private Branch otherBranch;
    private Employee targetEmployee;
    private User hrUser;
    private User sameBranchManager;
    private User otherBranchManager;
    private User selfEmployeeUser;
    private User otherEmployeeUser;
    private DisciplinaryCase openCase;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(1L);
        otherBranch = new Branch();
        otherBranch.setId(2L);

        targetEmployee = new Employee();
        targetEmployee.setId(4L);
        targetEmployee.setBranch(branch);

        hrUser = new User();
        hrUser.setUsername("hrtest2");
        hrUser.setRole("HR");

        Employee sameBranchManagerEmployee = new Employee();
        sameBranchManagerEmployee.setBranch(branch);
        sameBranchManager = new User();
        sameBranchManager.setUsername("mgrtest1");
        sameBranchManager.setRole("MANAGER");
        sameBranchManager.setEmployee(sameBranchManagerEmployee);

        Employee otherBranchManagerEmployee = new Employee();
        otherBranchManagerEmployee.setBranch(otherBranch);
        otherBranchManager = new User();
        otherBranchManager.setUsername("othermgr");
        otherBranchManager.setRole("MANAGER");
        otherBranchManager.setEmployee(otherBranchManagerEmployee);

        selfEmployeeUser = new User();
        selfEmployeeUser.setUsername("emptest1");
        selfEmployeeUser.setRole("EMPLOYEE");
        selfEmployeeUser.setEmployee(targetEmployee);

        Employee differentEmployee = new Employee();
        differentEmployee.setId(9L);
        otherEmployeeUser = new User();
        otherEmployeeUser.setUsername("otheremp");
        otherEmployeeUser.setRole("EMPLOYEE");
        otherEmployeeUser.setEmployee(differentEmployee);

        openCase = new DisciplinaryCase();
        openCase.setId(1L);
        openCase.setEmployee(targetEmployee);
        openCase.setCurrentStage(DisciplinaryStage.VERBAL_WARNING);
        openCase.setClosed(false);
    }

    // --- open ---

    @Test
    void open_asHr_createsCaseAndLogsHistory() {
        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();
        request.setEmployeeId(4L);
        request.setReason("Late arrival");
        request.setInitialStage("VERBAL_WARNING");

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));
        when(caseRepository.save(any(DisciplinaryCase.class))).thenAnswer(inv -> inv.getArgument(0));

        DisciplinaryCase result = disciplinaryCaseService.open(request, "hrtest2");

        assertThat(result.getEmployee()).isEqualTo(targetEmployee);
        assertThat(result.getCurrentStage()).isEqualTo(DisciplinaryStage.VERBAL_WARNING);
        assertThat(result.getOpenedBy()).isEqualTo(hrUser);
        verify(historyRepository).save(any(DisciplinaryCaseHistory.class));
    }

    @Test
    void open_invalidStage_throwsException() {
        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();
        request.setEmployeeId(4L);
        request.setInitialStage("NOT_A_STAGE");

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));

        assertThatThrownBy(() -> disciplinaryCaseService.open(request, "hrtest2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid stage: NOT_A_STAGE");
    }

    @Test
    void open_managerTryingHearingStage_throwsAccessDenied() {
        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();
        request.setEmployeeId(4L);
        request.setInitialStage("HEARING");

        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(sameBranchManager));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));

        assertThatThrownBy(() -> disciplinaryCaseService.open(request, "mgrtest1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Managers can only issue verbal or written warnings");
    }

    @Test
    void open_managerSameBranchVerbalWarning_succeeds() {
        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();
        request.setEmployeeId(4L);
        request.setReason("Late arrival");
        request.setInitialStage("VERBAL_WARNING");

        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(sameBranchManager));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));
        when(caseRepository.save(any(DisciplinaryCase.class))).thenAnswer(inv -> inv.getArgument(0));

        DisciplinaryCase result = disciplinaryCaseService.open(request, "mgrtest1");

        assertThat(result.getCurrentStage()).isEqualTo(DisciplinaryStage.VERBAL_WARNING);
    }

    @Test
    void open_managerDifferentBranch_throwsAccessDenied() {
        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();
        request.setEmployeeId(4L);
        request.setInitialStage("VERBAL_WARNING");

        when(userRepository.findByUsername("othermgr")).thenReturn(Optional.of(otherBranchManager));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));

        assertThatThrownBy(() -> disciplinaryCaseService.open(request, "othermgr"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Managers can only open cases for their own branch");
    }

    @Test
    void open_withLinkedEscalation_setsEscalation() {
        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();
        request.setEmployeeId(4L);
        request.setInitialStage("VERBAL_WARNING");
        request.setLinkedEscalationId(7L);

        Escalation escalation = new Escalation();
        escalation.setId(7L);

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));
        when(escalationRepository.findById(7L)).thenReturn(Optional.of(escalation));
        when(caseRepository.save(any(DisciplinaryCase.class))).thenAnswer(inv -> inv.getArgument(0));

        DisciplinaryCase result = disciplinaryCaseService.open(request, "hrtest2");

        assertThat(result.getLinkedEscalation()).isEqualTo(escalation);
    }

    // --- progressStage ---

    @Test
    void progressStage_closedCase_throwsException() {
        openCase.setClosed(true);

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(openCase));

        assertThatThrownBy(() -> disciplinaryCaseService.progressStage(1L, "RESOLVED", "x", null, "hrtest2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cannot progress a closed case");
    }

    @Test
    void progressStage_toResolved_marksClosed() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(openCase));
        when(caseRepository.save(any(DisciplinaryCase.class))).thenAnswer(inv -> inv.getArgument(0));

        DisciplinaryCase result = disciplinaryCaseService.progressStage(1L, "RESOLVED", "Resolved amicably", null, "hrtest2");

        assertThat(result.isClosed()).isTrue();
        assertThat(result.getCurrentStage()).isEqualTo(DisciplinaryStage.RESOLVED);
    }

    @Test
    void progressStage_toHearingStage_notClosed() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(openCase));
        when(caseRepository.save(any(DisciplinaryCase.class))).thenAnswer(inv -> inv.getArgument(0));

        DisciplinaryCase result = disciplinaryCaseService.progressStage(1L, "HEARING", "Scheduling hearing", null, "hrtest2");

        assertThat(result.isClosed()).isFalse();
        assertThat(result.getCurrentStage()).isEqualTo(DisciplinaryStage.HEARING);
    }

    @Test
    void progressStage_managerToHearing_throwsAccessDenied() {
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(sameBranchManager));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(openCase));

        assertThatThrownBy(() -> disciplinaryCaseService.progressStage(1L, "HEARING", "x", null, "mgrtest1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Managers can only issue verbal or written warnings");
    }

    @Test
    void progressStage_managerDifferentBranch_throwsAccessDenied() {
        when(userRepository.findByUsername("othermgr")).thenReturn(Optional.of(otherBranchManager));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(openCase));

        assertThatThrownBy(() -> disciplinaryCaseService.progressStage(1L, "WRITTEN_WARNING", "x", null, "othermgr"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Managers can only progress cases for their own branch");
    }

    @Test
    void progressStage_withLinkedHearing_setsHearingOnHistory() {
        Hearing hearing = new Hearing();
        hearing.setId(5L);

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(openCase));
        when(hearingRepository.findById(5L)).thenReturn(Optional.of(hearing));
        when(caseRepository.save(any(DisciplinaryCase.class))).thenAnswer(inv -> inv.getArgument(0));

        disciplinaryCaseService.progressStage(1L, "HEARING", "x", 5L, "hrtest2");

        verify(historyRepository).save(argThat(entry -> entry.getLinkedHearing() == hearing));
    }

    // --- getById ---

    @Test
    void getById_notFound_throwsException() {
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disciplinaryCaseService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Disciplinary case not found");
    }

    // --- getByEmployeeForViewer ---

    @Test
    void getByEmployeeForViewer_asHr_alwaysAllowed() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));
        when(caseRepository.findByEmployeeId(4L)).thenReturn(List.of(openCase));

        List<DisciplinaryCase> result = disciplinaryCaseService.getByEmployeeForViewer(4L, "hrtest2");

        assertThat(result).containsExactly(openCase);
        verify(employeeRepository, never()).findById(anyLong());
    }

    @Test
    void getByEmployeeForViewer_managerSameBranch_allowed() {
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(sameBranchManager));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));
        when(caseRepository.findByEmployeeId(4L)).thenReturn(List.of(openCase));

        List<DisciplinaryCase> result = disciplinaryCaseService.getByEmployeeForViewer(4L, "mgrtest1");

        assertThat(result).containsExactly(openCase);
    }

    @Test
    void getByEmployeeForViewer_managerDifferentBranch_throwsAccessDenied() {
        when(userRepository.findByUsername("othermgr")).thenReturn(Optional.of(otherBranchManager));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));

        assertThatThrownBy(() -> disciplinaryCaseService.getByEmployeeForViewer(4L, "othermgr"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Managers can only view cases for their own branch");
    }

    @Test
    void getByEmployeeForViewer_employeeViewingSelf_allowed() {
        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(selfEmployeeUser));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));
        when(caseRepository.findByEmployeeId(4L)).thenReturn(List.of(openCase));

        List<DisciplinaryCase> result = disciplinaryCaseService.getByEmployeeForViewer(4L, "emptest1");

        assertThat(result).containsExactly(openCase);
    }

    @Test
    void getByEmployeeForViewer_employeeViewingOther_throwsAccessDenied() {
        when(userRepository.findByUsername("otheremp")).thenReturn(Optional.of(otherEmployeeUser));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(targetEmployee));

        assertThatThrownBy(() -> disciplinaryCaseService.getByEmployeeForViewer(4L, "otheremp"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only view your own disciplinary case history");
    }

    // --- getHistory / getByEmployee / getByBranch / getAllOpen / getAll ---

    @Test
    void getHistory_returnsOrderedHistory() {
        DisciplinaryCaseHistory entry = new DisciplinaryCaseHistory();
        when(historyRepository.findByDisciplinaryCaseIdOrderByActionedAtAsc(1L)).thenReturn(List.of(entry));

        assertThat(disciplinaryCaseService.getHistory(1L)).containsExactly(entry);
    }

    @Test
    void getByEmployee_returnsCasesForEmployee() {
        when(caseRepository.findByEmployeeId(4L)).thenReturn(List.of(openCase));

        assertThat(disciplinaryCaseService.getByEmployee(4L)).containsExactly(openCase);
    }

    @Test
    void getByBranch_returnsCasesForBranch() {
        when(caseRepository.findByEmployeeBranchId(1L)).thenReturn(List.of(openCase));

        assertThat(disciplinaryCaseService.getByBranch(1L)).containsExactly(openCase);
    }

    @Test
    void getAllOpen_returnsOnlyOpenCases() {
        when(caseRepository.findByClosedFalse()).thenReturn(List.of(openCase));

        assertThat(disciplinaryCaseService.getAllOpen()).containsExactly(openCase);
    }

    @Test
    void getAll_returnsAllCases() {
        when(caseRepository.findAll()).thenReturn(List.of(openCase));

        assertThat(disciplinaryCaseService.getAll()).containsExactly(openCase);
    }
}