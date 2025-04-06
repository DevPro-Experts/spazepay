package com.spazepay.service;

import com.spazepay.dto.UpdateProfileRequest;
import com.spazepay.model.User;
import com.spazepay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User updateProfile(User user, UpdateProfileRequest request) {
        logger.info("Updating profile for user: {}", user.getEmail());
        user.setFullName(request.getFullName());
        user.setAddress(request.getAddress());
        User updatedUser = userRepository.save(user);
        logger.info("Profile updated for user: {}", updatedUser.getEmail());
        return updatedUser;
    }
}