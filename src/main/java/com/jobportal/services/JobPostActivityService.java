package com.jobportal.services;

import com.jobportal.entity.*;
import com.jobportal.repository.JobPostActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobPostActivityService {

    private final JobPostActivityRepository jobPostActivityRepository;

    @Autowired
    public JobPostActivityService(JobPostActivityRepository jobPostActivityRepository) {
        this.jobPostActivityRepository = jobPostActivityRepository;
    }

    public JobPostActivity addNew(JobPostActivity jobPostActivity) {
        return jobPostActivityRepository.save(jobPostActivity);
    }

    public List<RecruiterJobsDto> getRecruiterJobs(int recruiter) {

        List<IRecruiterJobs> recruiterJobsDto =
                jobPostActivityRepository.getRecruiterJobs(recruiter);

        List<RecruiterJobsDto> recruiterJobsDtoList = new ArrayList<>();

        for (IRecruiterJobs rec : recruiterJobsDto) {

            JobLocation loc = new JobLocation(
                    rec.getLocationId(),
                    rec.getCity(),
                    rec.getState(),
                    rec.getCountry()
            );

            JobCompany comp = new JobCompany(
                    rec.getCompanyId(),
                    rec.getName(),
                    ""
            );

            recruiterJobsDtoList.add(
                    new RecruiterJobsDto(
                            rec.getTotalCandidates(),
                            rec.getJob_post_id(),
                            rec.getJob_title(),
                            loc,
                            comp
                    )
            );
        }

        return recruiterJobsDtoList;
    }

    public JobPostActivity getOne(int id) {

        return jobPostActivityRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    public List<JobPostActivity> getAllActiveJobs() {

        return jobPostActivityRepository.findAll();
    }

    /*
     * CareerHub Search Engine
     */
    public List<JobPostActivity> searchJobs(String job, String location) {

        String cleanJob = cleanSearchText(job);
        String cleanLocation = cleanSearchText(location);

        return jobPostActivityRepository.searchJobs(
                cleanJob,
                cleanLocation
        );
    }

    /*
     * Prevent whitespace-only searches from causing
     * unexpected search behaviour.
     */
    private String cleanSearchText(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}