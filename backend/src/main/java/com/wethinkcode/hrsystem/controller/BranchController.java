package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.BranchRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.service.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    public ResponseEntity<Branch> create(@RequestBody BranchRequest request) {
        return ResponseEntity.ok(branchService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Branch>> getAll() {
        return ResponseEntity.ok(branchService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Branch> getById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Branch> update(@PathVariable Long id, @RequestBody BranchRequest request) {
        return ResponseEntity.ok(branchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
