package com.spazepay.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
}
