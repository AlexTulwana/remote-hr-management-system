package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.BranchRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchService branchService;

    private Branch existingBranch;

    @BeforeEach
    void setUp() {
        existingBranch = new Branch();
        existingBranch.setId(1L);
        existingBranch.setName("Cape Town");
        existingBranch.setAddress("123 Long Street");
    }

    @Test
    void create_savesNewBranch() {
        BranchRequest request = new BranchRequest();
        request.setName("Durban");
        request.setAddress("456 Beach Road");

        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        Branch result = branchService.create(request);

        assertThat(result.getName()).isEqualTo("Durban");
        assertThat(result.getAddress()).isEqualTo("456 Beach Road");
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void getAll_returnsAllBranches() {
        Branch second = new Branch();
        second.setId(2L);
        second.setName("Durban");

        when(branchRepository.findAll()).thenReturn(List.of(existingBranch, second));

        List<Branch> result = branchService.getAll();

        assertThat(result).hasSize(2).contains(existingBranch, second);
    }

    @Test
    void getById_found_returnsBranch() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(existingBranch));

        Branch result = branchService.getById(1L);

        assertThat(result).isEqualTo(existingBranch);
    }

    @Test
    void getById_notFound_throwsException() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Branch not found");
    }

    @Test
    void update_existingBranch_updatesFields() {
        BranchRequest request = new BranchRequest();
        request.setName("Cape Town Updated");
        request.setAddress("789 New Street");

        when(branchRepository.findById(1L)).thenReturn(Optional.of(existingBranch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        Branch result = branchService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Cape Town Updated");
        assertThat(result.getAddress()).isEqualTo("789 New Street");
    }

    @Test
    void update_nonExistentBranch_throwsException() {
        BranchRequest request = new BranchRequest();
        request.setName("Doesn't matter");

        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.update(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Branch not found");
    }

    @Test
    void delete_callsRepositoryDeleteById() {
        branchService.delete(1L);

        verify(branchRepository).deleteById(1L);
    }
}