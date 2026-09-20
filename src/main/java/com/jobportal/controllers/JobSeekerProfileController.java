package com.jobportal.controllers;

import com.jobportal.entity.JobSeekerProfile;
import com.jobportal.entity.Skills;
import com.jobportal.entity.Users;
import com.jobportal.repository.UsersRepository;
import com.jobportal.services.JobSeekerProfileService;
import com.jobportal.util.FileUploadUtil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/job-seeker-profile")
public class JobSeekerProfileController {

    private final UsersRepository usersRepository;
    private final JobSeekerProfileService jobSeekerProfileService;

    public JobSeekerProfileController(UsersRepository usersRepository, JobSeekerProfileService jobSeekerProfileService) {
        this.usersRepository = usersRepository;
        this.jobSeekerProfileService = jobSeekerProfileService;
    }

    @GetMapping("/")
    public String jobSeekerProfile(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();
            Users users = usersRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not found user"));
            Optional<JobSeekerProfile> jobSeekerProfile = jobSeekerProfileService.getOne(users.getUserId());

            JobSeekerProfile profile = jobSeekerProfile.orElseGet(() -> new JobSeekerProfile(users));
            model.addAttribute("profile", profile);

            String skillsCsv = "";
            if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
                skillsCsv = String.join(", ", profile.getSkills().stream().map(Skills::getName).toList());
            }
            model.addAttribute("skillsInput", skillsCsv);
        }

        return "job_seeker_profile";
    }

    @PostMapping("addNew")
    public String addNew(JobSeekerProfile jobSeekerProfile,
                          @RequestParam(value = "image", required = false) MultipartFile photoFile,
                          @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
                          @RequestParam(value = "skillsInput", required = false) String skillsInput,
                          Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();
            Users users = usersRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not found user"));
            jobSeekerProfile.setUserId(users);
            jobSeekerProfile.setUserAccountId(users.getUserId());
        }

        // Key skills come in as a single comma-separated field (like Naukri's
        // "Key Skills" tag input) and get expanded into Skills rows here.
        List<Skills> skills = new ArrayList<>();
        if (StringUtils.hasText(skillsInput)) {
            for (String rawSkill : skillsInput.split(",")) {
                String name = rawSkill.trim();
                if (!name.isEmpty()) {
                    Skills skill = new Skills();
                    skill.setName(name);
                    skill.setJobSeekerProfile(jobSeekerProfile);
                    skills.add(skill);
                }
            }
        }
        jobSeekerProfile.setSkills(skills);

        String photoFileName = "";
        if (photoFile != null && StringUtils.hasText(photoFile.getOriginalFilename())) {
            photoFileName = StringUtils.cleanPath(photoFile.getOriginalFilename());
            jobSeekerProfile.setProfilePhoto(photoFileName);
        }

        String resumeFileName = "";
        if (resumeFile != null && StringUtils.hasText(resumeFile.getOriginalFilename())) {
            resumeFileName = StringUtils.cleanPath(resumeFile.getOriginalFilename());
            jobSeekerProfile.setResume(resumeFileName);
        }

        JobSeekerProfile savedProfile = jobSeekerProfileService.addNew(jobSeekerProfile);

        String uploadDir = "photos/job-seeker/" + savedProfile.getUserAccountId();
        if (StringUtils.hasText(photoFileName)) {
            try {
                FileUploadUtil.saveFile(uploadDir, photoFileName, photoFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (StringUtils.hasText(resumeFileName)) {
            try {
                FileUploadUtil.saveFile(uploadDir, resumeFileName, resumeFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        model.addAttribute("profile", savedProfile);
        return "redirect:/dashboard/";
    }
}
