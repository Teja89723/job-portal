package com.jobportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Tracks a job that a job seeker has saved/bookmarked for later.
 * Maps to the existing `job_seeker_save` table (see DB_Scripts/01-jobportal.sql),
 * which already existed in the schema but had no JPA entity behind it.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "job_seeker_save", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job"}))
public class JobSeekerSave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "job")
    private JobPostActivity jobPostActivity;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private JobSeekerProfile userId;

    public JobSeekerSave(JobPostActivity jobPostActivity, JobSeekerProfile userId) {
        this.jobPostActivity = jobPostActivity;
        this.userId = userId;
    }
}
