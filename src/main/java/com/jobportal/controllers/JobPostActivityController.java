package com.jobportal.controllers;

import com.jobportal.entity.JobPostActivity;
import com.jobportal.entity.JobSeekerProfile;
import com.jobportal.entity.RecruiterJobsDto;
import com.jobportal.entity.RecruiterProfile;
import com.jobportal.entity.Users;
import com.jobportal.services.JobPostActivityService;
import com.jobportal.services.JobSeekerApplyService;
import com.jobportal.services.JobSeekerSaveService;
import com.jobportal.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Date;
import java.util.List;

@Controller
public class JobPostActivityController {
    private final UsersService usersService;
    private  final JobPostActivityService jobPostActivityService;
    private final JobSeekerSaveService jobSeekerSaveService;
    private final JobSeekerApplyService jobSeekerApplyService;
    @Autowired
    public JobPostActivityController(UsersService usersService, JobPostActivityService jobPostActivityService,
                                      JobSeekerSaveService jobSeekerSaveService, JobSeekerApplyService jobSeekerApplyService) {
        this.usersService = usersService;
        this.jobPostActivityService = jobPostActivityService;
        this.jobSeekerSaveService = jobSeekerSaveService;
        this.jobSeekerApplyService = jobSeekerApplyService;
    }

    @GetMapping("/dashboard/")
    public String searchJobs(Model model) {

        Object currentUserProfile = usersService.getCurrentUserProfile();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();
            model.addAttribute("username", currentUsername);
            if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))){
                List<RecruiterJobsDto> recruiterJobs = jobPostActivityService.getRecruiterJobs(((RecruiterProfile) currentUserProfile).getUserAccountId());
                model.addAttribute("jobPost", recruiterJobs);
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Job Seeker"))) {
                // Show every posted job to the job seeker, flagging which ones
                // they've already saved/applied to so the dashboard's
                // "(Saved)"/"(Applied)" labels and the Saved Jobs page stay in sync.
                List<JobPostActivity> allJobs = jobPostActivityService.getAllActiveJobs();
                if (currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {
                    for (JobPostActivity job : allJobs) {
                        job.setIsSaved(jobSeekerSaveService.isSaved(job.getJobPostId(), jobSeekerProfile.getUserAccountId()));
                        job.setIsActive(jobSeekerApplyService.hasApplied(job.getJobPostId(), jobSeekerProfile.getUserAccountId()));
                    }
                }
                model.addAttribute("jobPost", allJobs);
            }
        }

        model.addAttribute("user", currentUserProfile);

        return "dashboard";
    }

    @GetMapping("/dashboard/add")
    public String addJobs(Model model) {
        model.addAttribute("jobPostActivity", new JobPostActivity());
        model.addAttribute("user", usersService.getCurrentUserProfile());
        return "add-jobs";
    }

    @PostMapping("/dashboard/addNew")
    public String addNew(JobPostActivity jobPostActivity, Model model) {

        Users user = usersService.getCurrentUser();
        if (user != null) {
            jobPostActivity.setPostedById(user);
        }
        jobPostActivity.setPostedDate(new Date());
        model.addAttribute("jobPostActivity", jobPostActivity);
        jobPostActivityService.addNew(jobPostActivity);
        return "redirect:/dashboard/";
    }
    @GetMapping("dashboard/edit/{id}")
    public String editJob(@PathVariable("id") int id, Model model) {

        JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);
        model.addAttribute("jobPostActivity", jobPostActivity);
        model.addAttribute("user", usersService.getCurrentUserProfile());
        return "add-jobs";
    }
}
