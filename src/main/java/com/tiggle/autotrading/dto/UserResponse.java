package com.tiggle.autotrading.dto;

import com.tiggle.autotrading.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String userName,
        String mobile,
        String userType,
        Short usedBool,
        String recentIp,
        Integer failCount,
        Boolean locked,
        LocalDateTime createAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUserName(),
                user.getMobile(),
                user.getUserType(),
                user.getUsedBool(),
                user.getRecentIp(),
                user.getFailCount(),
                user.isLocked(),
                user.getCreateAt(),
                user.getUpdatedAt()
        );
    }
}
