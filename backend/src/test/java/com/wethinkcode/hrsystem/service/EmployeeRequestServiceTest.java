        package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeRequestSubmission;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRequestRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
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
class EmployeeRequestServiceTest {

    @Mock
    private EmployeeRequestRepository employeeRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeRequestService employeeRequestService;

    private Branch branch;
    private Employee employee;
    private User employeeUser;
    private User managerUser;
    private User otherBranchManagerUser;
    private User hrUser;
    private EmployeeRequest pendingRequest;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(1L);
        branch.setName("Cape Town");

        Branch otherBranch = new Branch();
        otherBranch.setId(2L);
        otherBranch.setName("Durban");

        employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        employee.setBranch(branch);

        employeeUser = new User();
        employeeUser.setUsername("emptest1");
        employeeUser.setRole("EMPLOYEE");
        employeeUser.setEmployee(employee);

        Employee managerEmployee = new Employee();
        managerEmployee.setId(3L);
        managerEmployee.setBranch(branch);

        managerUser = new User();
        managerUser.setUsername("mgrtest1");
        managerUser.setRole("MANAGER");
        managerUser.setEmployee(managerEmployee);

        Employee otherManagerEmployee = new Employee();
        otherManagerEmployee.setId(5L);
        otherManagerEmployee.setBranch(otherBranch);

        otherBranchManagerUser = new User();
        otherBranchManagerUser.setUsername("othermgr");
        otherBranchManagerUser.setRole("MANAGER");
        otherBranchManagerUser.setEmployee(otherManagerEmployee);

        hrUser = new User();
        hrUser.setUsername("hrtest2");
        hrUser.setRole("HR");

        pendingRequest = new EmployeeRequest();
        pendingRequest.setId(1L);
        pendingRequest.setEmployee(employee);
        pendingRequest.setStatus("PENDING");
    }

    // --- submit ---

    @Test
    void submit_validSubmission_savesRequest() {
        EmployeeRequestSubmission submission = new EmployeeRequestSubmission();
        submission.setRequestType("EQUIPMENT");
        submission.setDescription("Need a new laptop");

        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));
        when(employeeRequestRepository.save(any(EmployeeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRequest result = employeeRequestService.submit(submission, "emptest1");

        assertThat(result.getEmployee()).isEqualTo(employee);
        assertThat(result.getDescription()).isEqualTo("Need a new laptop");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void submit_unlinkedUser_throwsException() {
        User unlinkedUser = new User();
        unlinkedUser.setUsername("nolink");
        unlinkedUser.setEmployee(null);

        EmployeeRequestSubmission submission = new EmployeeRequestSubmission();
        submission.setRequestType("OTHER");

        when(userRepository.findByUsername("nolink")).thenReturn(Optional.of(unlinkedUser));

        assertThatThrownBy(() -> employeeRequestService.submit(submission, "nolink"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("This account is not linked to an employee record");
    }

    @Test
    void submit_invalidRequestType_throwsException() {
        EmployeeRequestSubmission submission = new EmployeeRequestSubmission();
        submission.setRequestType("NOT_A_REAL_TYPE");

        when(userRepository.findByUsername("emptest1")).thenReturn(Optional.of(employeeUser));

        assertThatThrownBy(() -> employeeRequestService.submit(submission, "emptest1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid request type: NOT_A_REAL_TYPE");
    }

    // --- managerDecision ---

    @Test
    void managerDecision_sameBranchApprove_updatesRequest() {
        when(employeeRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(managerUser));
        when(employeeRequestRepository.save(any(EmployeeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRequest result = employeeRequestService.managerDecision(
                1L, "APPROVED", "Looks fine", "mgrtest1"
        );

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getManagerComment()).isEqualTo("Looks fine");
        assertThat(result.getHandledBy()).isEqualTo(managerUser);
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void managerDecision_escalate_setsStatusEscalatedWithoutResolvedAt() {
        when(employeeRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(managerUser));
        when(employeeRequestRepository.save(any(EmployeeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRequest result = employeeRequestService.managerDecision(
                1L, "ESCALATED", "Needs HR review", "mgrtest1"
        );

        assertThat(result.getStatus()).isEqualTo("ESCALATED");
        assertThat(result.getResolvedAt()).isNull();
    }

    @Test
    void managerDecision_differentBranch_throwsAccessDenied() {
        when(employeeRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("othermgr")).thenReturn(Optional.of(otherBranchManagerUser));

        assertThatThrownBy(() ->
                employeeRequestService.managerDecision(
                        1L, "APPROVED", "x", "othermgr"
                )
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Managers can only action requests for their own branch");
    }

    @Test
    void managerDecision_nonPendingRequest_throwsException() {
        pendingRequest.setStatus("APPROVED");

        when(employeeRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(managerUser));

        assertThatThrownBy(() ->
                employeeRequestService.managerDecision(
                        1L, "REJECTED", "x", "mgrtest1"
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only pending requests can be actioned by a manager");
    }

    @Test
    void managerDecision_invalidDecision_throwsException() {
        when(employeeRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(managerUser));

        assertThatThrownBy(() ->
                employeeRequestService.managerDecision(
                        1L, "MAYBE", "x", "mgrtest1"
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid decision: MAYBE");
    }

    // --- hrDecision ---

    @Test
    void hrDecision_nonEscalatedRequest_throwsException() {
        pendingRequest.setStatus("PENDING");

        when(employeeRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));

        assertThatThrownBy(() ->
                employeeRequestService.hrDecision(
                        1L, "APPROVED", "x", "hrtest2"
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only escalated requests can be given a final HR decision");
    }

    @Test
    void hrDecision_invalidDecision_throwsException() {
        pendingRequest.setStatus("ESCALATED");

        when(employeeRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser));

        assertThatThrownBy(() ->
                employeeRequestService.hrDecision(
                        1L, "ESCALATED", "x", "hrtest2"
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid decision: ESCALATED");
    }

    // --- getters ---

    @Test
    void getById_notFound_throwsException() {
        when(employeeRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeRequestService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Request not found");
    }

    @Test
    void getByEmployee_returnsRequestsForEmployee() {
        when(employeeRequestRepository.findByEmployeeId(4L))
                .thenReturn(List.of(pendingRequest));

        assertThat(employeeRequestService.getByEmployee(4L))
                .containsExactly(pendingRequest);
    }

    @Test
    void getByBranch_returnsRequestsForBranch() {
        when(employeeRequestRepository.findByEmployeeBranchId(1L))
                .thenReturn(List.of(pendingRequest));

        assertThat(employeeRequestService.getByBranch(1L))
                .containsExactly(pendingRequest);
    }

    @Test
    void getAll_returnsAllRequests() {
        when(employeeRequestRepository.findAll())
                .thenReturn(List.of(pendingRequest));

        assertThat(employeeRequestService.getAll())
                .containsExactly(pendingRequest);
    }

    @Test
    void getEscalated_returnsOnlyEscalatedRequests() {
        when(employeeRequestRepository.findByStatus("ESCALATED"))
                .thenReturn(List.of(pendingRequest));

        assertThat(employeeRequestService.getEscalated())
                .containsExactly(pendingRequest);
    }

    // --- getById(Long, String) - new checked overload ---

    @Test
    void getById_self_returnsRequest() {
        when(employeeRequestRepository.findById(1L))
                .thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("emptest1"))
                .thenReturn(Optional.of(employeeUser));

        EmployeeRequest result =
                employeeRequestService.getById(1L, "emptest1");

        assertThat(result).isEqualTo(pendingRequest);
    }

    @Test
    void getById_managerSameBranch_returnsRequest() {
        when(employeeRequestRepository.findById(1L))
                .thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("mgrtest1"))
                .thenReturn(Optional.of(managerUser));

        EmployeeRequest result =
                employeeRequestService.getById(1L, "mgrtest1");

        assertThat(result).isEqualTo(pendingRequest);
    }

    @Test
    void getById_managerDifferentBranch_throwsAccessDenied() {
        when(employeeRequestRepository.findById(1L))
                .thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("othermgr"))
                .thenReturn(Optional.of(otherBranchManagerUser));

        assertThatThrownBy(() ->
                employeeRequestService.getById(1L, "othermgr")
        )
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_hr_returnsRequestRegardlessOfBranch() {
        when(employeeRequestRepository.findById(1L))
                .thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("hrtest2"))
                .thenReturn(Optional.of(hrUser));

        EmployeeRequest result =
                employeeRequestService.getById(1L, "hrtest2");

        assertThat(result).isEqualTo(pendingRequest);
    }

    @Test
    void getById_unrelatedEmployee_throwsAccessDenied() {
        Employee unrelatedEmployee = new Employee();
        unrelatedEmployee.setId(99L);
        unrelatedEmployee.setBranch(null);

        User unrelatedUser = new User();
        unrelatedUser.setUsername("unrelated");
        unrelatedUser.setEmployee(unrelatedEmployee);

        when(employeeRequestRepository.findById(1L))
                .thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("unrelated"))
                .thenReturn(Optional.of(unrelatedUser));

        assertThatThrownBy(() ->
                employeeRequestService.getById(1L, "unrelated")
        )
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- getByEmployee(Long, String) - new checked overload ---

    @Test
    void getByEmployeeChecked_self_returnsRequests() {
        when(employeeRepository.findById(4L))
                .thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("emptest1"))
                .thenReturn(Optional.of(employeeUser));
        when(employeeRequestRepository.findByEmployeeId(4L))
                .thenReturn(List.of(pendingRequest));

        List<EmployeeRequest> result =
                employeeRequestService.getByEmployee(4L, "emptest1");

        assertThat(result).containsExactly(pendingRequest);
    }

    @Test
    void getByEmployeeChecked_managerSameBranch_returnsRequests() {
        when(employeeRepository.findById(4L))
                .thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("mgrtest1"))
                .thenReturn(Optional.of(managerUser));
        when(employeeRequestRepository.findByEmployeeId(4L))
                .thenReturn(List.of(pendingRequest));

        List<EmployeeRequest> result =
                employeeRequestService.getByEmployee(4L, "mgrtest1");

        assertThat(result).containsExactly(pendingRequest);
    }

    @Test
    void getByEmployeeChecked_notSelfNotManagerNotHr_throwsAccessDenied() {
        when(employeeRepository.findById(4L))
                .thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("othermgr"))
                .thenReturn(Optional.of(otherBranchManagerUser));

        assertThatThrownBy(() ->
                employeeRequestService.getByEmployee(4L, "othermgr")
        )
                .isInstanceOf(AccessDeniedException.class);
    }
}
