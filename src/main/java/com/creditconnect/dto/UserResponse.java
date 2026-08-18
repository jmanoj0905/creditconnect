package com.creditconnect.dto;

import com.creditconnect.model.User;

import java.math.BigDecimal;

public record UserResponse(Long id, String name, String email, BigDecimal monthlyIncome) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getMonthlyIncome());
    }
}
