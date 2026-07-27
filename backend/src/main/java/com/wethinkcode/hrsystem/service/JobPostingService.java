package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.JobPostingRequest;
import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;

    public JobPostingService(JobPostingRepository jobPostingRepository, UserRepository userRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
    }

    public JobPosting create(JobPostingRequest request) {
        User postedBy = userRepository.findById(request.getPostedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        JobPosting posting = new JobPosting();
        posting.setTitle(request.getTitle());
        posting.setDescription(request.getDescription());
        posting.setRequirements(request.getRequirements());
        posting.setDepartment(request.getDepartment());
        posting.setStartDate(request.getStartDate());
        posting.setEndDate(request.getEndDate());
        posting.setMaxApplications(request.getMaxApplications());
        posting.setPostedBy(postedBy);

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