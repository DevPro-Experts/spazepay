package com.spazepay.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendAccountNumberEmail(String to, String accountNumber) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your Account Number");
        message.setText("Dear Customer,\n\nYour account has been created successfully. Your account number is: " + accountNumber + "\n\nRegards,\nBanking Team");
        mailSender.send(message);
    }

    /**
     * Email for successful login (unchanged from original).
     *
     * @param to        the to
     * @param fullName the full name
     */
    public void sendLoginEmail(String to, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom("test@example.com");
            helper.setTo(to);
            helper.setSubject("Successful Login");
            helper.setText(
                    "<html><body>" +
                            "<p>Dear " + fullName + ",</p>" +
                            "<p>You have successfully logged in to your account.</p>" +
                            "</body></html>",
                    true
            );
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error sending email to: " + to + ": " + e.getMessage());
        }
    }
}
