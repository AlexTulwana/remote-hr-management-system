package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.LeaveBalanceResponse;
import com.wethinkcode.hrsystem.dto.LeaveRequestDto;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    static final String ANNUAL = "ANNUAL";
    private static final List<String> COMMITTED_STATUSES = List.of("PENDING", "APPROVED");

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final String uploadDir = "uploads/leave-attachments/";
    private final CurrentUserService currentUserService;

    @Value("${leave.annual-days:15}")
    private int annualDays = 15;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository, CurrentUserService currentUserService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    public LeaveRequest submit(Long employeeId, LeaveRequestDto dto, MultipartFile attachment) {
        requireSelfOrHrAdmin(employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        validateDates(dto);
        List<LeaveRequest> committed = leaveRequestRepository.findByEmployeeIdAndStatusIn(employeeId, COMMITTED_STATUSES);
        requireNoOverlap(dto, committed);
        if (ANNUAL.equals(dto.getLeaveType())) {
            requireWithinAllowance(dto, committed);
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(dto.getLeaveType());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setReason(dto.getReason());
        leave.setStatus("PENDING");

        if (attachment != null && !attachment.isEmpty()) {
            leave.setAttachmentPath(saveAttachment(attachment));
        }

        return leaveRequestRepository.save(leave);
    }

    public LeaveRequest approve(Long leaveId) {
        LeaveRequest leave = getById(leaveId);
        requireManagerSameBranchOrHrAdmin(leave.getEmployee());
        requirePending(leave);
        leave.setStatus("APPROVED");
        recordDecision(leave);
        return leaveRequestRepository.save(leave);
    }

    public LeaveRequest reject(Long leaveId, String reason) {
        LeaveRequest leave = getById(leaveId);
        requireManagerSameBranchOrHrAdmin(leave.getEmployee());
        requirePending(leave);
        leave.setStatus("REJECTED");
        leave.setRejectionReason(reason);
        recordDecision(leave);
        return leaveRequestRepository.save(leave);
    }

    private void recordDecision(LeaveRequest leave) {
        User user = currentUserService.getCurrentUser();
        String name = user.getEmployee() != null && user.getEmployee().getFullName() != null
                ? user.getEmployee().getFullName() : user.getUsername();
        leave.setDecidedByName(name);
        leave.setDecidedAt(LocalDateTime.now());
    }

    private void requirePending(LeaveRequest leave) {
        if (!"PENDING".equals(leave.getStatus())) {
            throw new IllegalStateException("This leave request has already been " + leave.getStatus().toLowerCase());
        }
    }

    public LeaveRequest getById(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
    }

    public List<LeaveRequest> getByEmployee(Long employeeId) {
        String role = currentUserService.getCurrentUser().getRole();
        if (!role.equals("HR") && !role.equals("ADMIN")) {
            if (!currentUserService.isSelf(employeeId)) {
                throw new AccessDeniedException("You are not authorized to view these leave requests");
            }
        }
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getAll() {
        return leaveRequestRepository.findAll();
    }

    public List<LeaveRequest> getByBranch(Long branchId) {
        requireManagerOwnBranchOrHrAdmin(branchId);
        return leaveRequestRepository.findByEmployeeBranchId(branchId);
    }

    // ---------- attachments ----------

    public Path getAttachmentPath(Long leaveId) {
        LeaveRequest leave = getById(leaveId);
        requireCanViewAttachment(leave.getEmployee());
        if (leave.getAttachmentPath() == null) {
            throw new RuntimeException("This leave request has no attachment");
        }
        Path path = Paths.get(leave.getAttachmentPath());
        if (!Files.exists(path)) {
            throw new RuntimeException("Attachment file not found");
        }
        return path;
    }

    // ---------- balances ----------

    public LeaveBalanceResponse getBalance(Long employeeId) {
        requireSelfOrHrAdmin(employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        List<LeaveRequest> committed = leaveRequestRepository.findByEmployeeIdAndStatusIn(employeeId, COMMITTED_STATUSES);
        return buildBalance(employee, committed, LocalDate.now().getYear());
    }

    public List<LeaveBalanceResponse> getAllBalances() {
        if (!currentUserService.isHrOrAdmin()) {
            throw new AccessDeniedException("Only HR or Admin can view all leave balances");
        }
        int year = LocalDate.now().getYear();
        Map<Long, List<LeaveRequest>> byEmployee = leaveRequestRepository.findByStatusIn(COMMITTED_STATUSES).stream()
                .filter(l -> l.getEmployee() != null)
                .collect(Collectors.groupingBy(l -> l.getEmployee().getId()));
        return employeeRepository.findAll().stream()
                .filter(e -> "ACTIVE".equals(e.getEmploymentStatus()))
                .map(e -> buildBalance(e, byEmployee.getOrDefault(e.getId(), List.of()), year))
                .toList();
    }

    private LeaveBalanceResponse buildBalance(Employee employee, List<LeaveRequest> committed, int year) {
        int used = 0;
        int reserved = 0;
        int other = 0;
        for (LeaveRequest l : committed) {
            int days = weekdaysInYear(l, year);
            if (ANNUAL.equals(l.getLeaveType())) {
                if ("APPROVED".equals(l.getStatus())) used += days;
                else if ("PENDING".equals(l.getStatus())) reserved += days;
            } else if ("APPROVED".equals(l.getStatus())) {
                other += days;
            }
        }
        int remaining = Math.max(0, annualDays - used - reserved);
        return new LeaveBalanceResponse(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFullName(),
                employee.getBranch() != null ? employee.getBranch().getName() : null,
                year,
                annualDays,
                used,
                reserved,
                remaining,
                other
        );
    }

    // ---------- validation ----------

    private void validateDates(LeaveRequestDto dto) {
        LocalDate start = dto.getStartDate();
        LocalDate end = dto.getEndDate();
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end date are required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (start.getYear() != end.getYear()) {
            throw new IllegalArgumentException("A leave request cannot cross into a new year. Submit two separate requests.");
        }
        if (countWeekdays(start, end) == 0) {
            throw new IllegalArgumentException("The selected dates contain no working days (Mon-Fri)");
        }
    }

    private void requireNoOverlap(LeaveRequestDto dto, List<LeaveRequest> committed) {
        for (LeaveRequest l : committed) {
            if (l.getStartDate() == null || l.getEndDate() == null) continue;
            boolean overlaps = !l.getEndDate().isBefore(dto.getStartDate()) && !l.getStartDate().isAfter(dto.getEndDate());
            if (overlaps) {
                throw new IllegalStateException("These dates overlap with an existing "
                        + l.getStatus().toLowerCase() + " leave request");
            }
        }
    }

    private void requireWithinAllowance(LeaveRequestDto dto, List<LeaveRequest> committed) {
        int year = dto.getStartDate().getYear();
        int requested = countWeekdays(dto.getStartDate(), dto.getEndDate());
        int taken = 0;
        for (LeaveRequest l : committed) {
            if (ANNUAL.equals(l.getLeaveType())) {
                taken += weekdaysInYear(l, year);
            }
        }
        if (taken + requested > annualDays) {
            int remaining = Math.max(0, annualDays - taken);
            throw new IllegalStateException("Not enough annual leave for " + year + ": requested "
                    + requested + " working days but only " + remaining + " remain");
        }
    }

    static int countWeekdays(LocalDate start, LocalDate end) {
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek day = d.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) count++;
        }
        return count;
    }

    private static int weekdaysInYear(LeaveRequest leave, int year) {
        if (leave.getStartDate() == null || leave.getEndDate() == null) return 0;
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        LocalDate start = leave.getStartDate().isBefore(yearStart) ? yearStart : leave.getStartDate();
        LocalDate end = leave.getEndDate().isAfter(yearEnd) ? yearEnd : leave.getEndDate();
        return start.isAfter(end) ? 0 : countWeekdays(start, end);
    }

    // ---------- storage + access checks ----------

    private String saveAttachment(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment", e);
        }
    }

    private void requireSelfOrHrAdmin(Long employeeId) {
        String role = currentUserService.getCurrentUser().getRole();
        if (!role.equals("HR") && !role.equals("ADMIN")) {
            if (!currentUserService.isSelf(employeeId)) {
                throw new AccessDeniedException("You are not authorized to perform this action for this employee");
            }
        }
    }

    private void requireManagerSameBranchOrHrAdmin(Employee employee) {
        var currentUser = currentUserService.getCurrentUser();
        String role = currentUser.getRole();
        if (role.equals("HR") || role.equals("ADMIN")) return;
        if (role.equals("MANAGER")) {
            Long managerBranchId = currentUser.getEmployee() != null && currentUser.getEmployee().getBranch() != null
                    ? currentUser.getEmployee().getBranch().getId() : null;
            Long employeeBranchId = employee.getBranch() != null ? employee.getBranch().getId() : null;
            if (managerBranchId != null && managerBranchId.equals(employeeBranchId)) return;
        }
        throw new AccessDeniedException("Managers can only action leave requests for their own branch");
    }

    private void requireCanViewAttachment(Employee employee) {
        var currentUser = currentUserService.getCurrentUser();
        String role = currentUser.getRole();
        if (role.equals("HR") || role.equals("ADMIN")) return;
        if (employee != null && currentUserService.isSelf(employee.getId())) return;
        if (role.equals("MANAGER") && employee != null) {
            Long managerBranchId = currentUser.getEmployee() != null && currentUser.getEmployee().getBranch() != null
                    ? currentUser.getEmployee().getBranch().getId() : null;
            Long employeeBranchId = employee.getBranch() != null ? employee.getBranch().getId() : null;
            if (managerBranchId != null && managerBranchId.equals(employeeBranchId)) return;
        }
        throw new AccessDeniedException("You are not authorized to view this attachment");
    }

    private void requireManagerOwnBranchOrHrAdmin(Long branchId) {
        var currentUser = currentUserService.getCurrentUser();
        String role = currentUser.getRole();
        if (role.equals("HR") || role.equals("ADMIN")) return;
        if (role.equals("MANAGER")) {
            Long managerBranchId = currentUser.getEmployee() != null && currentUser.getEmployee().getBranch() != null
                    ? currentUser.getEmployee().getBranch().getId() : null;
            if (managerBranchId != null && managerBranchId.equals(branchId)) return;
        }
        throw new AccessDeniedException("You are not authorized to view leave requests for this branch");
    }
}
