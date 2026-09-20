package com.jobportal.controllers;

import com.jobportal.entity.JobPostActivity;
import com.jobportal.entity.JobSeekerApply;
import com.jobportal.entity.JobSeekerProfile;
import com.jobportal.services.JobPostActivityService;
import com.jobportal.services.JobSeekerApplyService;
import com.jobportal.services.JobSeekerSaveService;
import com.jobportal.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class JobSeekerApplyController {
    private final JobPostActivityService jobPostActivityService;
    private final UsersService userService;
    private final JobSeekerApplyService jobSeekerApplyService;
    private final JobSeekerSaveService jobSeekerSaveService;

    @Autowired
    public JobSeekerApplyController(JobPostActivityService jobPostActivityService, UsersService userService,
                                     JobSeekerApplyService jobSeekerApplyService, JobSeekerSaveService jobSeekerSaveService) {
        this.jobPostActivityService = jobPostActivityService;
        this.userService = userService;
        this.jobSeekerApplyService = jobSeekerApplyService;
        this.jobSeekerSaveService = jobSeekerSaveService;
    }

    @GetMapping("job-details-apply/{id}")
    public String display(@PathVariable("id") int id, Model model) {
        JobPostActivity jobDetails = jobPostActivityService.getOne(id);
        Object currentUserProfile = userService.getCurrentUserProfile();

        model.addAttribute("jobDetails", jobDetails);
        model.addAttribute("user", currentUserProfile);
        // Backs the (unused-but-required) th:object on the apply/save forms in job-details.html
        model.addAttribute("applyJob", new JobSeekerApply());

        boolean alreadyApplied = false;
        boolean alreadySaved = false;

        if (currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {
            alreadyApplied = jobSeekerApplyService.hasApplied(id, jobSeekerProfile.getUserAccountId());
            alreadySaved = jobSeekerSaveService.isSaved(id, jobSeekerProfile.getUserAccountId());
        }
        model.addAttribute("alreadyApplied", alreadyApplied);
        model.addAttribute("alreadySaved", alreadySaved);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {
            model.addAttribute("applyList", jobSeekerApplyService.getApplicantsForJob(id));
        }

        return "job-details";
    }

    @PostMapping("/job-details/apply/{id}")
    public String apply(@PathVariable("id") int id) {
        Object currentUserProfile = userService.getCurrentUserProfile();
        if (currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {
            JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);
            jobSeekerApplyService.apply(jobPostActivity, jobSeekerProfile);
        }
        return "redirect:/job-details-apply/" + id;
    }

    @PostMapping("/job-details/save/{id}")
    public String save(@PathVariable("id") int id) {
        Object currentUserProfile = userService.getCurrentUserProfile();
        if (currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {
            JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);
            jobSeekerSaveService.toggleSave(jobPostActivity, jobSeekerProfile);
        }
        return "redirect:/job-details-apply/" + id;
    }
}
