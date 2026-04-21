package com.tiggle.autotrading.dto;

public record SignupRequest(
        String email,
        String userName,
        String mobile,
        String userType,
        String password
) {}
