package com.creditconnect.service;

import com.creditconnect.dto.CreateUserRequest;
import com.creditconnect.dto.UserResponse;
import com.creditconnect.exception.InvalidLoanStateException;
import com.creditconnect.exception.ResourceNotFoundException;
import com.creditconnect.model.User;
import com.creditconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new InvalidLoanStateException("Email " + request.email() + " is already registered");
        }
        User user = userRepository.save(
                new User(request.name(), request.email(), request.monthlyIncome()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional(readOnly = true)
    public User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }
}
