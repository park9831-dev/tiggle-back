package com.tiggle.autotrading.dto;

public record UserUpdateRequest(
        String userName,
        String mobile,
        String userType,
        Short usedBool
) {}
