package com.easyschedule.backend.shared.admin;

import com.easyschedule.backend.auth.models.User;

public final class AdminUser {

    public static final String USERNAME = "Admin";
    public static final String EMAIL = "user.admin@easyschedule.com";
    public static final String DEFAULT_PASSWORD = "AdminPassword123@";

    private AdminUser() {
    }

    public static boolean isAdmin(User user) {
        if (user == null || user.getUsername() == null || user.getEmail() == null) {
            return false;
        }

        return USERNAME.equalsIgnoreCase(user.getUsername().trim())
            && EMAIL.equalsIgnoreCase(user.getEmail().trim());
    }
}
