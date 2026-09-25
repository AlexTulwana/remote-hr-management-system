package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.BranchRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final JobPostingRepository jobPostingRepository;

    public BranchService(BranchRepository branchRepository,
                          EmployeeRepository employeeRepository,
                          JobPostingRepository jobPostingRepository) {
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
        this.jobPostingRepository = jobPostingRepository;
    }

    public Branch create(BranchRequest request) {
        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        return branchRepository.save(branch);
    }

    public List<Branch> getAll() {
        return branchRepository.findAll();
    }

    public Branch getById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
    }

    public Branch update(Long id, BranchRequest request) {
        Branch branch = getById(id);
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        return branchRepository.save(branch);
    }

    public void delete(Long id) {
        long employeeCount = employeeRepository.countByBranchId(id);
        long jobPostingCount = jobPostingRepository.countByBranchId(id);

        if (employeeCount > 0 || jobPostingCount > 0) {
            List<String> parts = new ArrayList<>();
            if (employeeCount > 0) {
                parts.add(employeeCount + (employeeCount == 1 ? " employee" : " employees"));
            }
            if (jobPostingCount > 0) {
                parts.add(jobPostingCount + (jobPostingCount == 1 ? " job posting" : " job postings"));
            }
            throw new IllegalStateException(
                    "Cannot delete this branch: it still has " + String.join(" and ", parts) + " assigned to it");
        }

        branchRepository.deleteById(id);
    }
}
