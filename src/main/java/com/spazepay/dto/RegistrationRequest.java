package com.spazepay.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegistrationRequest {
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String nationality;
    private String phoneNumber;
    private String email;
    private String bvnOrNin;
    private String passportPhoto;

    // Default constructor
    public RegistrationRequest() {}
}
