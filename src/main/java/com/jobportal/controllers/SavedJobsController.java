package com.jobportal.controllers;

import com.jobportal.entity.JobPostActivity;
import com.jobportal.entity.JobSeekerProfile;
import com.jobportal.services.JobSeekerSaveService;
import com.jobportal.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Collections;
import java.util.List;

@Controller
public class SavedJobsController {

    private final UsersService usersService;
    private final JobSeekerSaveService jobSeekerSaveService;

    @Autowired
    public SavedJobsController(UsersService usersService, JobSeekerSaveService jobSeekerSaveService) {
        this.usersService = usersService;
        this.jobSeekerSaveService = jobSeekerSaveService;
    }

    @GetMapping("/saved-jobs/")
    public String savedJobs(Model model) {
        Object currentUserProfile = usersService.getCurrentUserProfile();
        model.addAttribute("user", currentUserProfile);

        List<JobPostActivity> savedJobs = Collections.emptyList();
        if (currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {
            savedJobs = jobSeekerSaveService.getSavedJobsForUser(jobSeekerProfile.getUserAccountId());
        }
        model.addAttribute("savedJobs", savedJobs);

        return "saved-jobs";
    }

    @PostMapping("/saved-jobs/remove/{id}")
    public String remove(@PathVariable("id") int id) {
        Object currentUserProfile = usersService.getCurrentUserProfile();
        if (currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {
            jobSeekerSaveService.unsave(id, jobSeekerProfile.getUserAccountId());
        }
        return "redirect:/saved-jobs/";
    }
}
