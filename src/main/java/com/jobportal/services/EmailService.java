package com.jobportal.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("CareerHub - Password Reset OTP");

        message.setText(
                "Hello,\n\n" +
                "We received a request to reset your CareerHub password.\n\n" +
                "Your One-Time Password (OTP) is:\n\n" +
                otp + "\n\n" +
                "This OTP is valid for 10 minutes and can only be used once.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Regards,\n" +
                "CareerHub Team"
        );

        mailSender.send(message);
    }
}