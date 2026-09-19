package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeRequest;
import com.wethinkcode.hrsystem.dto.UpdateContactRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Branch branch;
    private Employee existingEmployee;
    private Employee manager;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(1L);
        branch.setName("Cape Town");

        existingEmployee = new Employee();
        existingEmployee.setId(4L);
        existingEmployee.setEmployeeNumber("EMP004");
        existingEmployee.setFullName("Emma Employee");
        existingEmployee.setBranch(branch);

        manager = new Employee();
        manager.setId(3L);
        manager.setEmployeeNumber("EMP003");
        manager.setFullName("Sam Manager");
    }

    // --- create ---

    @Test
    void create_withBranch_savesEmployee() {
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployeeNumber("EMP005");
        request.setFullName("New Person");
        request.setPosition("Analyst");
        request.setDepartment("IT");
        request.setBranchId(1L);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = employeeService.create(request);

        assertThat(result.getEmployeeNumber()).isEqualTo("EMP005");
        assertThat(result.getFullName()).isEqualTo("New Person");
        assertThat(result.getBranch()).isEqualTo(branch);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void create_withUnknownBranch_throwsException() {
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployeeNumber("EMP005");
        request.setBranchId(99L);

        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Branch not found");
    }

    @Test
    void create_withReportsTo_setsManager() {
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployeeNumber("EMP005");
        request.setFullName("New Person");
        request.setReportsToId(3L);

        when(employeeRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = employeeService.create(request);

        assertThat(result.getReportsTo()).isEqualTo(manager);
    }

    @Test
    void create_reportingToUnknownEmployee_throwsException() {
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployeeNumber("EMP005");
        request.setReportsToId(99L);

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Reports-to employee not found");
    }

    // --- getAll / getById ---

    @Test
    void getAll_returnsAllEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of(existingEmployee, manager));

        List<Employee> result = employeeService.getAll();

        assertThat(result).hasSize(2).contains(existingEmployee, manager);
    }

    @Test
    void getById_found_returnsEmployee() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(existingEmployee));

        Employee result = employeeService.getById(4L);

        assertThat(result).isEqualTo(existingEmployee);
    }

    @Test
    void getById_notFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Employee not found");
    }

    // --- update ---

    @Test
    void update_existingEmployee_updatesFields() {
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployeeNumber("EMP004");
        request.setFullName("Emma Updated");
        request.setPosition("Senior Analyst");
        request.setDepartment("IT");
        request.setEmploymentDate(LocalDate.of(2026, 1, 1));

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = employeeService.update(4L, request);

        assertThat(result.getFullName()).isEqualTo("Emma Updated");
        assertThat(result.getPosition()).isEqualTo("Senior Analyst");
    }

    @Test
    void update_reportingToSelf_throwsException() {
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployeeNumber("EMP004");
        request.setReportsToId(4L); // same as existingEmployee's own id

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(existingEmployee));

        assertThatThrownBy(() -> employeeService.update(4L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("An employee cannot report to themselves");
    }

    // --- linkUserToEmployee ---

    @Test
    void linkUserToEmployee_success() {
        User user = new User();
        user.setId(10L);
        user.setUsername("newuser");

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(existingEmployee));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        employeeService.linkUserToEmployee(4L, 10L);

        assertThat(user.getEmployee()).isEqualTo(existingEmployee);
        verify(userRepository).save(user);
    }

    @Test
    void linkUserToEmployee_unknownUser_throwsException() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(existingEmployee));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.linkUserToEmployee(4L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // --- deactivate ---

    @Test
    void deactivate_setsActiveFalseAndSaves() {
        existingEmployee.setActive(true);

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        employeeService.deactivate(4L);

        assertThat(existingEmployee.isActive()).isFalse();
        verify(employeeRepository).save(existingEmployee);
    }

    // ---- updateOwnContact() ----

    private UpdateContactRequest contactRequest(String contactDetails, String email) {
        UpdateContactRequest request = new UpdateContactRequest();
        request.setContactDetails(contactDetails);
        request.setEmail(email);
        return request;
    }

    @Test
    void updateOwnContact_valid_trimsAndSaves() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(existingEmployee));

        employeeService.updateOwnContact(4L, contactRequest("  082 555 0000  ", "  new@example.com  "));

        assertThat(existingEmployee.getContactDetails()).isEqualTo("082 555 0000");
        assertThat(existingEmployee.getEmail()).isEqualTo("new@example.com");
        verify(employeeRepository).save(existingEmployee);
    }

    @Test
    void updateOwnContact_blankContactDetails_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> employeeService.updateOwnContact(4L, contactRequest("   ", "new@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Contact details are required");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateOwnContact_nullContactDetails_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> employeeService.updateOwnContact(4L, contactRequest(null, "new@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Contact details are required");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateOwnContact_blankEmail_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> employeeService.updateOwnContact(4L, contactRequest("082 555 0000", "   ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email is required");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateOwnContact_nullEmail_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> employeeService.updateOwnContact(4L, contactRequest("082 555 0000", null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email is required");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateOwnContact_invalidEmail_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> employeeService.updateOwnContact(4L, contactRequest("082 555 0000", "not-an-email")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email address is not valid");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateOwnContact_unknownEmployee_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateOwnContact(99L, contactRequest("082 555 0000", "new@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Employee not found");
        verify(employeeRepository, never()).save(any());
    }
}
