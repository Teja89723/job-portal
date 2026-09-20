package com.jobportal.repository;

import com.jobportal.entity.JobSeekerApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobSeekerApplyRepository extends JpaRepository<JobSeekerApply, Integer> {

    boolean existsByJobPostActivity_JobPostIdAndUserId_UserAccountId(int jobPostId, int userAccountId);

    List<JobSeekerApply> findByJobPostActivity_JobPostId(int jobPostId);

    List<JobSeekerApply> findByUserId_UserAccountId(int userAccountId);
}
