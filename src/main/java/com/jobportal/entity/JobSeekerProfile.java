package com.jobportal.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "Job_seeker_profile")
public class JobSeekerProfile {

    @Id
    private int  userAccountId;

    @OneToOne
    @JoinColumn(name = "user_account_id")
    @MapsId
    private Users userId;

    // ===== Basic details =====
    private String firstName;
    private String lastName;
    private String phoneNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date dateOfBirth;
    private String gender;

    private String city;
    private String state;
    private String country;
    private String workAuthorazation;
    private String employmentType;

    // ===== Career details (Naukri-style) =====
    private String currentDesignation;
    private String currentCompany;
    /** e.g. "2 years 6 months" */
    private String totalExperience;
    private String currentCtc;
    private String expectedCtc;
    private String noticePeriod;

    // ===== Education =====
    private String highestQualification;
    private String university;
    private Integer graduationYear;

    // ===== Profile summary / about =====
    @Column(length = 3000)
    private String profileSummary;

    private String linkedinUrl;

    // ===== Uploads =====
    private String resume;
    @Column(nullable = true, length = 64)
    private String profilePhoto;

    @OneToMany(targetEntity = Skills.class, mappedBy = "jobSeekerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skills> skills;

    public JobSeekerProfile(Users users) {
        this.userId = users;
    }

    @Transient
    public String getPhotosImagePath() {
        if (profilePhoto == null || profilePhoto.isBlank()) return null;
        return "/photos/job-seeker/" + userAccountId + "/" + profilePhoto;
    }

    @Transient
    public String getResumePath() {
        if (resume == null || resume.isBlank()) return null;
        return "/photos/job-seeker/" + userAccountId + "/" + resume;
    }
}
