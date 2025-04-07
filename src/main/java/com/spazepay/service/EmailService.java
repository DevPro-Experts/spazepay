package com.spazepay.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.sender}") // Inject sender email from config
    private String senderEmail;

    @Async // Make email sending asynchronous
    public void sendAccountNumberEmail(String to, String accountNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail); // Use configured sender
            message.setTo(to);
            message.setSubject("Your Account Number");
            message.setText("Dear Customer,\n\nYour account has been created successfully. Your account number is: " + accountNumber + "\n\nRegards,\nBanking Team");
            mailSender.send(message);
            logger.info("Account number email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send account number email to: {}", to, e);
        }
    }

    @Async // Make email sending asynchronous
    public void sendLoginEmail(String to, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(senderEmail); // Use configured sender
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
            logger.info("Login email sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send login email to: {}", to, e);
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("HTML email sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email to: {}", to, e);
        }
    }
}