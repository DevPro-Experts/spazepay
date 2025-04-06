package com.spazepay.service;

import com.spazepay.dto.RegistrationRequest;
import com.spazepay.model.Account;
import com.spazepay.model.User;
import com.spazepay.repository.AccountRepository;
import com.spazepay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    public void register(RegistrationRequest request) {
        logger.info("Processing registration for email: {}", request.getEmail());

        // Check for existing email or phone number
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Email already registered: {}", request.getEmail());
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            logger.warn("Phone number already registered: {}", request.getPhoneNumber());
            throw new IllegalArgumentException("Phone number is already registered");
        }

        // Generate account number
        String accountNumber = generateAccountNumber(request.getPhoneNumber());
        logger.info("Generated account number: {}", accountNumber);

        // Check for existing account number
        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            logger.warn("Account number {} already exists. Consider handling duplicates.", accountNumber);
            throw new IllegalStateException("Account number already exists for phone number: " + request.getPhoneNumber());
        }

        // Create and save user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setNationality(request.getNationality());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setBvnOrNin(request.getBvnOrNin());
        user.setPassportPhoto(request.getPassportPhoto());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        userRepository.save(user);
        logger.info("User saved with ID: {}", user.getId());

        // Create and save account
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        accountRepository.save(account);
        logger.info("Account saved with number: {}", accountNumber);

        // Send email with account number
        try {
            emailService.sendAccountNumberEmail(user.getEmail(), accountNumber);
            logger.info("Email sent to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Registration succeeded, but email notification failed", e);
        }
    }

    private String validateAndFormatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        // Handle international format (+234) or local format (0)
        if (phoneNumber.startsWith("+234")) {
            phoneNumber = "0" + phoneNumber.substring(4);
        } else if (phoneNumber.startsWith("234")) {
            phoneNumber = "0" + phoneNumber.substring(3);
        }

        // Validate Nigerian phone number: 11 digits starting with 0
        if (!phoneNumber.matches("^0[7-9][0-1]\\d{8}$")) {
            throw new IllegalArgumentException("Invalid Nigerian phone number format. Must be 11 digits starting with 0 (e.g., 08012345678)");
        }

        return phoneNumber;
    }

    private String generateAccountNumber(String phoneNumber) {
        // Remove leading 0 to create a 10-digit account number
        if (phoneNumber.startsWith("0")) {
            return phoneNumber.substring(1);
        }
        logger.error("Invalid phone number format for account generation: {}", phoneNumber);
        throw new IllegalArgumentException("Phone number must start with 0");
    }
}