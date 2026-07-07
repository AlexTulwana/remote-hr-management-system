package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.BranchRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
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
        branchRepository.deleteById(id);
    }
}
