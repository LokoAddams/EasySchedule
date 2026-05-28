package com.easyschedule.backend.auth.service;

public record GoogleUserInfo(
        String googleId,
        String email,
        String name
) {
}