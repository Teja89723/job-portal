package com.jobportal.repository;

import com.jobportal.entity.IRecruiterJobs;
import com.jobportal.entity.JobPostActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobPostActivityRepository extends JpaRepository<JobPostActivity, Integer> {

    @Query(value = """
            SELECT COUNT(s.user_id) as totalCandidates,
                   j.job_post_id,
                   j.job_title,
                   l.id as locationId,
                   l.city,
                   l.state,
                   l.country,
                   c.id as companyId,
                   c.name
            FROM job_post_activity j
            INNER JOIN job_location l
                ON j.job_location_id = l.id
            INNER JOIN job_company c
                ON j.job_company_id = c.id
            LEFT JOIN job_seeker_apply s
                ON s.job = j.job_post_id
            WHERE j.posted_by_id = :recruiter
            GROUP BY j.job_post_id
            """, nativeQuery = true)
    List<IRecruiterJobs> getRecruiterJobs(@Param("recruiter") int recruiter);


    /*
     * CareerHub Job Search Engine
     *
     * Searches:
     * - Job title
     * - Job description
     * - Company name
     * - City
     * - State
     * - Country
     * - Remote work mode
     */
    @Query("""
            SELECT j
            FROM JobPostActivity j
            LEFT JOIN j.jobLocationId l
            LEFT JOIN j.jobCompanyId c
            WHERE
                (
                    :job IS NULL
                    OR :job = ''
                    OR LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :job, '%'))
                    OR LOWER(j.descriptionOfJob) LIKE LOWER(CONCAT('%', :job, '%'))
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :job, '%'))
                )
                AND
                (
                    :location IS NULL
                    OR :location = ''
                    OR LOWER(l.city) LIKE LOWER(CONCAT('%', :location, '%'))
                    OR LOWER(l.state) LIKE LOWER(CONCAT('%', :location, '%'))
                    OR LOWER(l.country) LIKE LOWER(CONCAT('%', :location, '%'))
                    OR LOWER(j.remote) LIKE LOWER(CONCAT('%', :location, '%'))
                )
            ORDER BY j.postedDate DESC
            """)
    List<JobPostActivity> searchJobs(
            @Param("job") String job,
            @Param("location") String location
    );
}