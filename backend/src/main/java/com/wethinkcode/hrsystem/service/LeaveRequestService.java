package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.LeaveRequestDto;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final String uploadDir = "uploads/leave-attachments/";
    private final CurrentUserService currentUserService;


    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository, CurrentUserService currentUserService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    public LeaveRequest submit(Long employeeId, LeaveRequestDto dto, MultipartFile attachment) {
        requireSelfOrHrAdmin(employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

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
        leave.setStatus("APPROVED");
        return leaveRequestRepository.save(leave);
    }

    public LeaveRequest reject(Long leaveId, String reason) {
        LeaveRequest leave = getById(leaveId);
        requireManagerSameBranchOrHrAdmin(leave.getEmployee());
        leave.setStatus("REJECTED");
        leave.setRejectionReason(reason);
        return leaveRequestRepository.save(leave);
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