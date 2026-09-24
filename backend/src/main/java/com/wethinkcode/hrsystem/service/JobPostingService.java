package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.JobPostingRequest;
import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.stereotype.Service;
import com.wethinkcode.hrsystem.dto.EmailTemplateUpdateRequest;


import java.time.LocalDate;
import java.util.List;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final BranchRepository branchRepository;

    public JobPostingService(JobPostingRepository jobPostingRepository, UserRepository userRepository, CurrentUserService currentUserService, BranchRepository branchRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.branchRepository = branchRepository;
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId == null) return null;
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
    }

    public JobPosting create(JobPostingRequest request) {
        User postedBy = currentUserService.getCurrentUser();

        JobPosting posting = new JobPosting();
        posting.setTitle(request.getTitle());
        posting.setDescription(request.getDescription());
        posting.setRequirements(request.getRequirements());
        posting.setDepartment(request.getDepartment());
        posting.setStartDate(request.getStartDate());
        posting.setEndDate(request.getEndDate());
        posting.setMaxApplications(request.getMaxApplications());
        posting.setPostedBy(postedBy);
        posting.setBranch(resolveBranch(request.getBranchId()));
        if (request.getRequiredDocuments() != null) {
            posting.setRequiredDocuments(request.getRequiredDocuments());
        }

        return jobPostingRepository.save(posting);
    }

    public JobPosting update(Long id, JobPostingRequest request) {
        JobPosting posting = getById(id);

        posting.setTitle(request.getTitle());
        posting.setDescription(request.getDescription());
        posting.setRequirements(request.getRequirements());
        posting.setDepartment(request.getDepartment());
        posting.setStartDate(request.getStartDate());
        posting.setEndDate(request.getEndDate());
        posting.setMaxApplications(request.getMaxApplications());
        posting.setBranch(resolveBranch(request.getBranchId()));
        if (request.getRequiredDocuments() != null) {
            posting.setRequiredDocuments(request.getRequiredDocuments());
        }

        return jobPostingRepository.save(posting);
    }

    public JobPosting updateEmailTemplates(Long id, EmailTemplateUpdateRequest request) {
        JobPosting posting = getById(id);

        if (request.getRejectedEmailTemplate() != null) {
            posting.setRejectedEmailTemplate(request.getRejectedEmailTemplate());
        }
        if (request.getInterviewInviteEmailTemplate() != null) {
            posting.setInterviewInviteEmailTemplate(request.getInterviewInviteEmailTemplate());
        }
        if (request.getAcceptedEmailTemplate() != null) {
            posting.setAcceptedEmailTemplate(request.getAcceptedEmailTemplate());
        }

        return jobPostingRepository.save(posting);
    }

    public List<JobPosting> getOpen() {
        LocalDate today = LocalDate.now();
        return jobPostingRepository.findAll().stream()
                .filter(p -> !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()))
                .toList();
    }

    public List<JobPosting> getAll() {
        return jobPostingRepository.findAll();
    }

    public JobPosting getById(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job posting not found"));
    }

    public void delete(Long id) {
        jobPostingRepository.deleteById(id);
    }
}