package com.jobportal.services;

import com.jobportal.entity.JobPostActivity;
import com.jobportal.entity.JobSeekerProfile;
import com.jobportal.entity.JobSeekerSave;
import com.jobportal.repository.JobSeekerSaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobSeekerSaveService {

    private final JobSeekerSaveRepository jobSeekerSaveRepository;

    @Autowired
    public JobSeekerSaveService(JobSeekerSaveRepository jobSeekerSaveRepository) {
        this.jobSeekerSaveRepository = jobSeekerSaveRepository;
    }

    public boolean isSaved(int jobPostId, int jobSeekerAccountId) {
        return jobSeekerSaveRepository.existsByJobPostActivity_JobPostIdAndUserId_UserAccountId(jobPostId, jobSeekerAccountId);
    }

    /**
     * Saves the job if it isn't already saved, or un-saves it if it is.
     * @return true if the job is now saved, false if it was just removed.
     */
    @Transactional
    public boolean toggleSave(JobPostActivity jobPostActivity, JobSeekerProfile jobSeekerProfile) {
        int jobPostId = jobPostActivity.getJobPostId();
        int jobSeekerAccountId = jobSeekerProfile.getUserAccountId();

        if (isSaved(jobPostId, jobSeekerAccountId)) {
            jobSeekerSaveRepository.deleteByJobPostActivity_JobPostIdAndUserId_UserAccountId(jobPostId, jobSeekerAccountId);
            return false;
        } else {
            jobSeekerSaveRepository.save(new JobSeekerSave(jobPostActivity, jobSeekerProfile));
            return true;
        }
    }

    @Transactional
    public void unsave(int jobPostId, int jobSeekerAccountId) {
        jobSeekerSaveRepository.deleteByJobPostActivity_JobPostIdAndUserId_UserAccountId(jobPostId, jobSeekerAccountId);
    }

    public List<JobPostActivity> getSavedJobsForUser(int jobSeekerAccountId) {
        return jobSeekerSaveRepository.findByUserId_UserAccountId(jobSeekerAccountId)
                .stream()
                .map(JobSeekerSave::getJobPostActivity)
                .collect(Collectors.toList());
    }
}
