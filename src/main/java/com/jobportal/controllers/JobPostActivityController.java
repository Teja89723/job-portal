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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

@Controller
public class JobPostActivityController {

    private final UsersService usersService;
    private final JobPostActivityService jobPostActivityService;
    private final JobSeekerSaveService jobSeekerSaveService;
    private final JobSeekerApplyService jobSeekerApplyService;

    @Autowired
    public JobPostActivityController(
            UsersService usersService,
            JobPostActivityService jobPostActivityService,
            JobSeekerSaveService jobSeekerSaveService,
            JobSeekerApplyService jobSeekerApplyService) {

        this.usersService = usersService;
        this.jobPostActivityService = jobPostActivityService;
        this.jobSeekerSaveService = jobSeekerSaveService;
        this.jobSeekerApplyService = jobSeekerApplyService;
    }


    /*
     * ============================================================
     * JOB SEEKER / RECRUITER DASHBOARD
     * ============================================================
     *
     * Supports:
     *
     * /dashboard/
     * /dashboard/?job=Java
     * /dashboard/?location=Bangalore
     * /dashboard/?job=Java&location=Bangalore
     */
    @GetMapping("/dashboard/")
    public String searchJobs(
            @RequestParam(required = false) String job,
            @RequestParam(required = false) String location,
            Model model) {

        Object currentUserProfile = usersService.getCurrentUserProfile();

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {

            String currentUsername = authentication.getName();

            model.addAttribute("username", currentUsername);

            /*
             * ====================================================
             * RECRUITER
             * ====================================================
             */
            if (authentication.getAuthorities()
                    .contains(new SimpleGrantedAuthority("Recruiter"))) {

                List<RecruiterJobsDto> recruiterJobs =
                        jobPostActivityService.getRecruiterJobs(
                                ((RecruiterProfile) currentUserProfile)
                                        .getUserAccountId()
                        );

                model.addAttribute("jobPost", recruiterJobs);
            }


            /*
             * ====================================================
             * JOB SEEKER
             * ====================================================
             */
            else if (authentication.getAuthorities()
                    .contains(new SimpleGrantedAuthority("Job Seeker"))) {

                List<JobPostActivity> jobs;

                /*
                 * If the user searched something,
                 * use the CareerHub Search Engine.
                 *
                 * Otherwise show all jobs.
                 */
                if ((job != null && !job.trim().isEmpty())
                        || (location != null && !location.trim().isEmpty())) {

                    jobs = jobPostActivityService.searchJobs(
                            job,
                            location
                    );

                } else {

                    jobs = jobPostActivityService.getAllActiveJobs();
                }


                /*
                 * Add Saved / Applied status
                 * for the current Job Seeker.
                 */
                if (currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {

                    for (JobPostActivity jobPost : jobs) {

                        jobPost.setIsSaved(
                                jobSeekerSaveService.isSaved(
                                        jobPost.getJobPostId(),
                                        jobSeekerProfile.getUserAccountId()
                                )
                        );

                        jobPost.setIsActive(
                                jobSeekerApplyService.hasApplied(
                                        jobPost.getJobPostId(),
                                        jobSeekerProfile.getUserAccountId()
                                )
                        );
                    }
                }

                model.addAttribute("jobPost", jobs);
            }
        }

        /*
         * Search values are returned to the page so the
         * search fields keep their entered values.
         */
        model.addAttribute("job", job);
        model.addAttribute("location", location);
        model.addAttribute("user", currentUserProfile);

        return "dashboard";
    }


    /*
     * ============================================================
     * PUBLIC CAREERHUB SEARCH ENGINE
     * ============================================================
     *
     * Used by the search box on the home page.
     *
     * Example:
     *
     * /global-search/?job=Java
     *
     * /global-search/?job=Java&location=Bangalore
     */
    @GetMapping("/global-search/")
    public String globalSearch(
            @RequestParam(required = false) String job,
            @RequestParam(required = false) String location,
            Model model) {

        Object currentUserProfile =
                usersService.getCurrentUserProfile();

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();


        /*
         * Search the database.
         */
        List<JobPostActivity> jobs =
                jobPostActivityService.searchJobs(
                        job,
                        location
                );


        /*
         * If a Job Seeker is already logged in,
         * show Saved / Applied status.
         */
        if (!(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("Job Seeker"))
                && currentUserProfile instanceof JobSeekerProfile jobSeekerProfile) {

            for (JobPostActivity jobPost : jobs) {

                jobPost.setIsSaved(
                        jobSeekerSaveService.isSaved(
                                jobPost.getJobPostId(),
                                jobSeekerProfile.getUserAccountId()
                        )
                );

                jobPost.setIsActive(
                        jobSeekerApplyService.hasApplied(
                                jobPost.getJobPostId(),
                                jobSeekerProfile.getUserAccountId()
                        )
                );
            }
        }


        /*
         * Send everything to dashboard.
         */
        model.addAttribute("jobPost", jobs);
        model.addAttribute("job", job);
        model.addAttribute("location", location);
        model.addAttribute("user", currentUserProfile);


        /*
         * If logged in, keep username available
         * for the dashboard header.
         */
        if (!(authentication instanceof AnonymousAuthenticationToken)) {

            model.addAttribute(
                    "username",
                    authentication.getName()
            );
        }

        return "dashboard";
    }


    /*
     * ============================================================
     * ADD NEW JOB
     * ============================================================
     */
    @GetMapping("/dashboard/add")
    public String addJobs(Model model) {

        model.addAttribute(
                "jobPostActivity",
                new JobPostActivity()
        );

        model.addAttribute(
                "user",
                usersService.getCurrentUserProfile()
        );

        return "add-jobs";
    }


    /*
     * ============================================================
     * SAVE NEW JOB
     * ============================================================
     */
    @PostMapping("/dashboard/addNew")
    public String addNew(
            JobPostActivity jobPostActivity,
            Model model) {

        Users user = usersService.getCurrentUser();

        if (user != null) {
            jobPostActivity.setPostedById(user);
        }

        jobPostActivity.setPostedDate(new Date());

        model.addAttribute(
                "jobPostActivity",
                jobPostActivity
        );

        jobPostActivityService.addNew(jobPostActivity);

        return "redirect:/dashboard/";
    }


    /*
     * ============================================================
     * EDIT JOB
     * ============================================================
     */
    @GetMapping("dashboard/edit/{id}")
    public String editJob(
            @PathVariable("id") int id,
            Model model) {

        JobPostActivity jobPostActivity =
                jobPostActivityService.getOne(id);

        model.addAttribute(
                "jobPostActivity",
                jobPostActivity
        );

        model.addAttribute(
                "user",
                usersService.getCurrentUserProfile()
        );

        return "add-jobs";
    }
}