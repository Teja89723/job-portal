package com.jobportal.services;

import com.jobportal.entity.JobPostActivity;
import com.jobportal.entity.JobSeekerApply;
import com.jobportal.entity.JobSeekerProfile;
import com.jobportal.repository.JobSeekerApplyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobSeekerApplyService {

    private final JobSeekerApplyRepository jobSeekerApplyRepository;

    @Autowired
    public JobSeekerApplyService(JobSeekerApplyRepository jobSeekerApplyRepository) {
        this.jobSeekerApplyRepository = jobSeekerApplyRepository;
    }

    public boolean hasApplied(int jobPostId, int jobSeekerAccountId) {
        return jobSeekerApplyRepository.existsByJobPostActivity_JobPostIdAndUserId_UserAccountId(jobPostId, jobSeekerAccountId);
    }

    public void apply(JobPostActivity jobPostActivity, JobSeekerProfile jobSeekerProfile) {
        if (!hasApplied(jobPostActivity.getJobPostId(), jobSeekerProfile.getUserAccountId())) {
            jobSeekerApplyRepository.save(new JobSeekerApply(jobPostActivity, jobSeekerProfile));
        }
    }

    public List<JobSeekerApply> getApplicantsForJob(int jobPostId) {
        return jobSeekerApplyRepository.findByJobPostActivity_JobPostId(jobPostId);
    }

    public List<JobSeekerApply> getApplicationsForJobSeeker(int jobSeekerAccountId) {
        return jobSeekerApplyRepository.findByUserId_UserAccountId(jobSeekerAccountId);
    }
}
