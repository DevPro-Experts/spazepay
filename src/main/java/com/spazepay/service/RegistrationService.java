package com.spazepay.service;

import com.spazepay.dto.RegistrationRequest;
import com.spazepay.model.Account;
import com.spazepay.model.User;
import com.spazepay.repository.AccountRepository;
import com.spazepay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    public void register(RegistrationRequest request) {
        // Validate and format phone number
        String phoneNumber = validateAndFormatPhoneNumber(request.getPhoneNumber());
        logger.info("Validated phone number: {}", phoneNumber);

        // Generate account number
        String accountNumber = generateAccountNumber(phoneNumber);
        logger.info("Generated account number: {}", accountNumber);

        // Check for existing account number
        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            logger.warn("Account number {} already exists. Consider handling duplicates.", accountNumber);
            throw new IllegalStateException("Account number already exists for phone number: " + phoneNumber);
        }

        // Create and save user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setNationality(request.getNationality());
        user.setPhoneNumber(phoneNumber);
        user.setEmail(request.getEmail());
        user.setBvnOrNin(request.getBvnOrNin());
        user.setPassportPhoto(request.getPassportPhoto());
        userRepository.save(user);
        logger.info("User saved with email: {}", user.getEmail());

        // Create and save account
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        accountRepository.save(account);
        logger.info("Account saved with number: {}", accountNumber);

        // Send email with account number
        emailService.sendAccountNumberEmail(user.getEmail(), accountNumber);
        logger.info("Email sent to: {}", user.getEmail());
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
        throw new IllegalArgumentException("Phone number must start with 0");
    }
}