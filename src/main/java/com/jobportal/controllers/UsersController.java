package com.jobportal.controllers;

import com.jobportal.entity.Users;
import com.jobportal.entity.UsersType;
import com.jobportal.services.UsersService;
import com.jobportal.services.UsersTypeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class UsersController {

    private final UsersTypeService usersTypeService;
    private final UsersService usersService;

    @Autowired
    public UsersController(
            UsersTypeService usersTypeService,
            UsersService usersService) {

        this.usersTypeService = usersTypeService;
        this.usersService = usersService;
    }


    // ========================================================================
    // REGISTRATION
    // ========================================================================

    @GetMapping("/register")
    public String register(
            Model model,
            HttpSession session) {

        // Clear any previous incomplete registration
        session.removeAttribute("pendingRegistration");

        List<UsersType> usersTypes =
                usersTypeService.getAll();

        model.addAttribute(
                "getAllTypes",
                usersTypes
        );

        model.addAttribute(
                "user",
                new Users()
        );

        return "register";
    }


    // ========================================================================
    // REGISTRATION - SEND EMAIL VERIFICATION OTP
    // ========================================================================

    @PostMapping("/register/new")
    public String userRegistration(
            @Valid Users users,
            BindingResult result,
            Model model,
            HttpSession session) {

        if (result.hasErrors()) {

            List<UsersType> usersTypes =
                    usersTypeService.getAll();

            model.addAttribute(
                    "getAllTypes",
                    usersTypes
            );

            model.addAttribute(
                    "user",
                    users
            );

            return "register";
        }


        // Normalize email
        String email =
                users.getEmail()
                        .trim()
                        .toLowerCase();

        users.setEmail(email);


        // Check whether email already exists
        boolean otpSent =
                usersService.sendRegistrationVerificationOtp(email);

        if (!otpSent) {

            List<UsersType> usersTypes =
                    usersTypeService.getAll();

            model.addAttribute(
                    "getAllTypes",
                    usersTypes
            );

            model.addAttribute(
                    "user",
                    users
            );

            model.addAttribute(
                    "error",
                    "An account with this email already exists."
            );

            return "register";
        }


        /*
         * IMPORTANT:
         *
         * Keep the registration information in the server-side session
         * until the email OTP has been successfully verified.
         *
         * The account is NOT inserted into the database yet.
         */
        session.setAttribute(
                "pendingRegistration",
                users
        );


        model.addAttribute(
                "email",
                email
        );

        model.addAttribute(
                "message",
                "A verification OTP has been sent to your email."
        );

        return "verify-registration-otp";
    }


    // ========================================================================
    // REGISTRATION OTP PAGE
    // ========================================================================

    @GetMapping("/verify-registration-otp")
    public String verifyRegistrationOtpPage(
            @RequestParam(
                    value = "email",
                    required = false
            ) String email,
            Model model,
            HttpSession session) {

        Users pendingUser =
                (Users) session.getAttribute(
                        "pendingRegistration"
                );


        // No pending registration
        if (pendingUser == null) {
            return "redirect:/register";
        }


        if (email == null || email.trim().isEmpty()) {

            email = pendingUser.getEmail();
        }


        String normalizedEmail =
                email.trim().toLowerCase();


        // Make sure the email belongs to the pending registration
        if (!normalizedEmail.equals(
                pendingUser.getEmail().trim().toLowerCase())) {

            return "redirect:/register";
        }


        model.addAttribute(
                "email",
                normalizedEmail
        );

        return "verify-registration-otp";
    }


    // ========================================================================
    // REGISTRATION OTP VERIFICATION
    // ========================================================================

    @PostMapping("/verify-registration-otp")
    public String verifyRegistrationOtp(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            Model model,
            HttpSession session) {

        String normalizedEmail =
                email.trim().toLowerCase();


        // Retrieve pending registration
        Users pendingUser =
                (Users) session.getAttribute(
                        "pendingRegistration"
                );


        // No pending registration found
        if (pendingUser == null) {

            return "redirect:/register";
        }


        // Security check:
        // OTP email must match the pending registration email
        String pendingEmail =
                pendingUser.getEmail()
                        .trim()
                        .toLowerCase();

        if (!pendingEmail.equals(normalizedEmail)) {

            model.addAttribute(
                    "email",
                    pendingEmail
            );

            model.addAttribute(
                    "error",
                    "Registration session expired or invalid. Please register again."
            );

            return "verify-registration-otp";
        }


        // Verify OTP
        boolean verified =
                usersService.verifyRegistrationEmailOtp(
                        normalizedEmail,
                        otp
                );


        // OTP invalid or expired
        if (!verified) {

            model.addAttribute(
                    "email",
                    normalizedEmail
            );

            model.addAttribute(
                    "error",
                    "Invalid or expired OTP. Please request a new OTP."
            );

            return "verify-registration-otp";
        }


        /*
         * IMPORTANT:
         *
         * OTP is valid.
         *
         * NOW create the actual user account.
         *
         * UsersService.addNew() will:
         * 1. Activate the account
         * 2. Encode the password
         * 3. Save the user
         * 4. Create the Job Seeker/Recruiter profile
         */
        usersService.addNew(pendingUser);


        // Remove temporary registration information
        session.removeAttribute(
                "pendingRegistration"
        );


        return "registration-complete";
    }


    // ========================================================================
    // LOGIN
    // ========================================================================

    @GetMapping("/login")
    public String login() {

        return "login";
    }


    // ========================================================================
    // FORGOT PASSWORD - ENTER EMAIL
    // ========================================================================

    @GetMapping("/forgot-password")
    public String forgotPassword() {

        return "forgot-password";
    }


    // ========================================================================
    // FORGOT PASSWORD - SEND OTP
    // ========================================================================

    @PostMapping("/forgot-password/send-otp")
    public String sendPasswordResetOtp(
            @RequestParam("email") String email,
            Model model) {

        if (email == null || email.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Please enter your registered email address."
            );

            return "forgot-password";
        }


        boolean otpSent =
                usersService.sendPasswordResetOtp(email);


        if (!otpSent) {

            model.addAttribute(
                    "error",
                    "No account was found with this email address."
            );

            return "forgot-password";
        }


        model.addAttribute(
                "email",
                email.trim().toLowerCase()
        );

        return "verify-otp";
    }


    // ========================================================================
    // PASSWORD RESET - VERIFY OTP PAGE
    // ========================================================================

    @GetMapping("/verify-otp")
    public String verifyOtpPage(
            @RequestParam(
                    value = "email",
                    required = false
            ) String email,
            Model model) {

        if (email == null || email.trim().isEmpty()) {

            return "redirect:/forgot-password";
        }


        model.addAttribute(
                "email",
                email.trim().toLowerCase()
        );

        return "verify-otp";
    }


    // ========================================================================
    // PASSWORD RESET - VERIFY OTP
    // ========================================================================

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            Model model) {

        boolean validOtp =
                usersService.verifyPasswordResetOtp(
                        email,
                        otp
                );


        if (!validOtp) {

            model.addAttribute(
                    "email",
                    email.trim().toLowerCase()
            );

            model.addAttribute(
                    "error",
                    "Invalid or expired OTP. Please request a new OTP."
            );

            return "verify-otp";
        }


        model.addAttribute(
                "email",
                email.trim().toLowerCase()
        );

        model.addAttribute(
                "otp",
                otp.trim()
        );

        return "reset-password";
    }


    // ========================================================================
    // PASSWORD RESET PAGE
    // ========================================================================

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam(
                    value = "email",
                    required = false
            ) String email,
            @RequestParam(
                    value = "otp",
                    required = false
            ) String otp,
            Model model) {

        if (email == null
                || email.trim().isEmpty()
                || otp == null
                || otp.trim().isEmpty()) {

            return "redirect:/forgot-password";
        }


        model.addAttribute(
                "email",
                email.trim().toLowerCase()
        );

        model.addAttribute(
                "otp",
                otp.trim()
        );

        return "reset-password";
    }


    // ========================================================================
    // PASSWORD RESET
    // ========================================================================

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        if (newPassword == null
                || newPassword.length() < 8) {

            model.addAttribute(
                    "email",
                    email
            );

            model.addAttribute(
                    "otp",
                    otp
            );

            model.addAttribute(
                    "error",
                    "Password must contain at least 8 characters."
            );

            return "reset-password";
        }


        if (!newPassword.equals(confirmPassword)) {

            model.addAttribute(
                    "email",
                    email
            );

            model.addAttribute(
                    "otp",
                    otp
            );

            model.addAttribute(
                    "error",
                    "Passwords do not match."
            );

            return "reset-password";
        }


        boolean passwordReset =
                usersService.resetPassword(
                        email,
                        otp,
                        newPassword
                );


        if (!passwordReset) {

            model.addAttribute(
                    "email",
                    email
            );

            model.addAttribute(
                    "otp",
                    otp
            );

            model.addAttribute(
                    "error",
                    "Invalid or expired OTP. Please request a new OTP."
            );

            return "reset-password";
        }


        return "redirect:/login?resetSuccess=true";
    }


    // ========================================================================
    // LOGOUT
    // ========================================================================

    @GetMapping("/logout")
    public String logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication != null) {

            new SecurityContextLogoutHandler()
                    .logout(
                            request,
                            response,
                            authentication
                    );
        }


        return "redirect:/";
    }
}