package com.jobportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Tracks a job seeker applying to a job post.
 * Maps to the existing `job_seeker_apply` table (see DB_Scripts/01-jobportal.sql),
 * which already existed in the schema but had no JPA entity behind it.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "job_seeker_apply", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job"}))
public class JobSeekerApply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "job")
    private JobPostActivity jobPostActivity;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private JobSeekerProfile userId;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date applyDate;

    private String coverLetter;

    public JobSeekerApply(JobPostActivity jobPostActivity, JobSeekerProfile userId) {
        this.jobPostActivity = jobPostActivity;
        this.userId = userId;
        this.applyDate = new Date();
    }
}
