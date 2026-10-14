
package com.example.backend.user.dto;

import java.util.UUID;

public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;

    public AuthResponse(String accessToken, long expiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public UserInfo getUser() {
        return user;
    }

    public static class UserInfo {

        private UUID id;
        private String fullName;
        private String phone;
        private String status;

        public UserInfo(UUID id, String fullName, String phone, String status) {
            this.id = id;
            this.fullName = fullName;
            this.phone = phone;
            this.status = status;
        }

        public UUID getId() {
            return id;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPhone() {
            return phone;
        }

        public String getStatus() {
            return status;
        }
    }
}
