package com.spazepay.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegistrationRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    private String gender;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "Nationality is required")
    @Size(min = 2, max = 50, message = "Nationality must be between 2 and 50 characters")
    private String nationality;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[7-9][0-1]\\d{8}$", message = "Phone number must be a valid 11-digit Nigerian number starting with 0 (e.g., 08012345678)")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "BVN or NIN is required")
    @Size(min = 11, max = 11, message = "BVN or NIN must be exactly 11 digits")
    @Pattern(regexp = "^\\d{11}$", message = "BVN or NIN must be numeric")
    private String bvnOrNin;

    @NotBlank(message = "Passport photo is required")
    private String passportPhoto;

    @NotBlank(message = "Pin is required")
    @Size(min = 4, max = 4, message = "Pin must be 4 digits")
    @Pattern(regexp = "\\d{4}", message = "Pin must be numeric")
    private String pin;
}
