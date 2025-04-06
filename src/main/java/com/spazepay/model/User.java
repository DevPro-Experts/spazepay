package com.spazepay.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Setter
@Getter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    public User() {}
}