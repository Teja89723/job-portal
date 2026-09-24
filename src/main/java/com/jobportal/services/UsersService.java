package com.jobportal.services;

import com.jobportal.entity.EmailVerificationToken;
import com.jobportal.entity.JobSeekerProfile;
import com.jobportal.entity.PasswordResetToken;
import com.jobportal.entity.RecruiterProfile;
import com.jobportal.entity.Users;
import com.jobportal.entity.UsersType;
import com.jobportal.repository.EmailVerificationTokenRepository;
import com.jobportal.repository.JobSeekerProfileRepository;
import com.jobportal.repository.PasswordResetTokenRepository;
import com.jobportal.repository.RecruiterProfileRepository;
import com.jobportal.repository.UsersRepository;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

@Service
public class UsersService {

    private final UsersRepository usersRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final EntityManager entityManager;

    @Autowired
    public UsersService(
            UsersRepository usersRepository,
            JobSeekerProfileRepository jobSeekerProfileRepository,
            RecruiterProfileRepository recruiterProfileRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            EmailService emailService,
            EntityManager entityManager) {

        this.usersRepository = usersRepository;
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
        this.entityManager = entityManager;
    }

    // ========================================================================
    // REGISTRATION
    // ========================================================================

    @Transactional
    public Users addNew(Users users) {

        /*
         * UsersType already exists in the database.
         * Get a managed reference instead of trying to persist a detached entity.
         */
        if (users.getUserTypeId() != null) {

            int userTypeId =
                    users.getUserTypeId().getUserTypeId();

            UsersType managedUserType =
                    entityManager.getReference(
                            UsersType.class,
                            userTypeId
                    );

            users.setUserTypeId(managedUserType);
        }

        // Activate account
        users.setActive(true);

        // Registration date
        users.setRegistrationDate(new Date());

        // Encode password exactly once
        String rawPassword = users.getPassword();

        if (rawPassword != null && !rawPassword.isBlank()) {
            users.setPassword(
                    passwordEncoder.encode(rawPassword)
            );
        }

        // Save user
        Users savedUsers =
                usersRepository.save(users);

        // Get user type
        int userTypeId =
                savedUsers.getUserTypeId().getUserTypeId();

        // Create appropriate profile
        if (userTypeId == 1) {

            recruiterProfileRepository.save(
                    new RecruiterProfile(savedUsers)
            );

        } else {

            jobSeekerProfileRepository.save(
                    new JobSeekerProfile(savedUsers)
            );
        }

        return savedUsers;
    }


    // ========================================================================
    // CURRENT USER PROFILE
    // ========================================================================

    public Object getCurrentUserProfile() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {

            String username = authentication.getName();

            Users users =
                    usersRepository.findByEmail(username)
                            .orElseThrow(() ->
                                    new UsernameNotFoundException(
                                            "Could not find user"
                                    )
                            );

            int userId = users.getUserId();

            if (authentication.getAuthorities()
                    .contains(new SimpleGrantedAuthority("Recruiter"))) {

                RecruiterProfile recruiterProfile =
                        recruiterProfileRepository
                                .findById(userId)
                                .orElse(new RecruiterProfile());

                return recruiterProfile;

            } else {

                JobSeekerProfile jobSeekerProfile =
                        jobSeekerProfileRepository
                                .findById(userId)
                                .orElse(new JobSeekerProfile());

                return jobSeekerProfile;
            }
        }

        return null;
    }


    // ========================================================================
    // CURRENT USER
    // ========================================================================

    public Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {

            String username = authentication.getName();

            return usersRepository.findByEmail(username)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "Could not find user"
                            )
                    );
        }

        return null;
    }


    // ========================================================================
    // PASSWORD RESET - SEND OTP
    // ========================================================================

    public boolean sendPasswordResetOtp(String email) {

        String normalizedEmail =
                email.trim().toLowerCase();

        Users user =
                usersRepository.findByEmail(normalizedEmail)
                        .orElse(null);

        if (user == null) {
            return false;
        }

        passwordResetTokenRepository
                .deleteByEmail(normalizedEmail);

        String otp =
                String.format(
                        "%06d",
                        new Random().nextInt(1_000_000)
                );

        PasswordResetToken token =
                new PasswordResetToken();

        token.setEmail(normalizedEmail);
        token.setOtp(otp);
        token.setExpiryTime(
                LocalDateTime.now().plusMinutes(10)
        );
        token.setUsed(false);

        passwordResetTokenRepository.save(token);

        emailService.sendOtpEmail(
                normalizedEmail,
                otp
        );

        return true;
    }


    // ========================================================================
    // PASSWORD RESET - VERIFY OTP
    // ========================================================================

    public boolean verifyPasswordResetOtp(
            String email,
            String otp) {

        String normalizedEmail =
                email.trim().toLowerCase();

        PasswordResetToken token =
                passwordResetTokenRepository
                        .findTopByEmailOrderByIdDesc(normalizedEmail)
                        .orElse(null);

        if (token == null) {
            return false;
        }

        if (token.isUsed()) {
            return false;
        }

        if (token.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            return false;
        }

        return token.getOtp().equals(otp.trim());
    }


    // ========================================================================
    // PASSWORD RESET - CHANGE PASSWORD
    // ========================================================================

    public boolean resetPassword(
            String email,
            String otp,
            String newPassword) {

        String normalizedEmail =
                email.trim().toLowerCase();

        PasswordResetToken token =
                passwordResetTokenRepository
                        .findTopByEmailOrderByIdDesc(normalizedEmail)
                        .orElse(null);

        if (token == null) {
            return false;
        }

        if (token.isUsed()) {
            return false;
        }

        if (token.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            return false;
        }

        if (!token.getOtp().equals(otp.trim())) {
            return false;
        }

        Users user =
                usersRepository.findByEmail(normalizedEmail)
                        .orElse(null);

        if (user == null) {
            return false;
        }

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        usersRepository.save(user);

        token.setUsed(true);

        passwordResetTokenRepository.save(token);

        return true;
    }


    // ========================================================================
    // REGISTRATION EMAIL VERIFICATION - SEND OTP
    // ========================================================================

    public boolean sendRegistrationVerificationOtp(
            String email) {

        String normalizedEmail =
                email.trim().toLowerCase();

        // Do not allow registration if email already exists
        if (usersRepository.findByEmail(normalizedEmail)
                .isPresent()) {

            return false;
        }

        // Remove previous registration OTP
        emailVerificationTokenRepository
                .deleteByEmail(normalizedEmail);

        // Generate 6-digit OTP
        String otp =
                String.format(
                        "%06d",
                        new Random().nextInt(1_000_000)
                );

        EmailVerificationToken token =
                new EmailVerificationToken();

        token.setEmail(normalizedEmail);
        token.setOtp(otp);
        token.setExpiryTime(
                LocalDateTime.now().plusMinutes(10)
        );
        token.setVerified(false);

        emailVerificationTokenRepository.save(token);

        // Send verification OTP
        emailService.sendOtpEmail(
                normalizedEmail,
                otp
        );

        return true;
    }


    // ========================================================================
    // REGISTRATION EMAIL VERIFICATION - VERIFY OTP
    // ========================================================================

    public boolean verifyRegistrationEmailOtp(
            String email,
            String otp) {

        String normalizedEmail =
                email.trim().toLowerCase();

        EmailVerificationToken token =
                emailVerificationTokenRepository
                        .findTopByEmailOrderByIdDesc(normalizedEmail)
                        .orElse(null);

        if (token == null) {
            return false;
        }

        if (token.isVerified()) {
            return false;
        }

        if (token.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            return false;
        }

        if (!token.getOtp().equals(otp.trim())) {
            return false;
        }

        token.setVerified(true);

        emailVerificationTokenRepository.save(token);

        return true;
    }


    // ========================================================================
    // CHECK EMAIL VERIFICATION
    // ========================================================================

    public boolean isEmailVerificationValid(
            String email) {

        String normalizedEmail =
                email.trim().toLowerCase();

        EmailVerificationToken token =
                emailVerificationTokenRepository
                        .findTopByEmailOrderByIdDesc(normalizedEmail)
                        .orElse(null);

        if (token == null) {
            return false;
        }

        if (!token.isVerified()) {
            return false;
        }

        if (token.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            return false;
        }

        return true;
    }
}