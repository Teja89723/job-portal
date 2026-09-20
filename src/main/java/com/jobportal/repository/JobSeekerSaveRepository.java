package com.jobportal.repository;

import com.jobportal.entity.JobSeekerSave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobSeekerSaveRepository extends JpaRepository<JobSeekerSave, Integer> {

    boolean existsByJobPostActivity_JobPostIdAndUserId_UserAccountId(int jobPostId, int userAccountId);

    Optional<JobSeekerSave> findByJobPostActivity_JobPostIdAndUserId_UserAccountId(int jobPostId, int userAccountId);

    List<JobSeekerSave> findByUserId_UserAccountId(int userAccountId);

    void deleteByJobPostActivity_JobPostIdAndUserId_UserAccountId(int jobPostId, int userAccountId);
}
