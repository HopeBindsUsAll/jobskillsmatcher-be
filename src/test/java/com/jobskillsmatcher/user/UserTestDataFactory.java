package com.jobskillsmatcher.user;

import com.jobskillsmatcher.user.impl.jpa.AdminProfile;
import com.jobskillsmatcher.user.impl.jpa.StudentProfile;
import com.jobskillsmatcher.user.impl.jpa.User;
import com.jobskillsmatcher.user.model.Role;

import java.util.UUID;

public class UserTestDataFactory {

    public final static UUID STUDENT_ID = UUID.fromString("ac2af47f-139e-4b46-b0ba-2a135fd8181a");

    public final static UUID ADMIN_ID = UUID.fromString("ce9bfde1-aa73-4031-8e1d-b06f675f24c4");

    public final static String STUDENT_EMAIL = "student@test.io";

    public final static String ADMIN_EMAIL = "admin@test.io";

    public final static String PASSWORD = "Password123";

    public final static String PASSWORD_HASH = "{bcrypt}hashed-password";

    public static User student() {
        User user = new User();
        user.setId(STUDENT_ID);
        user.setEmail(STUDENT_EMAIL);
        user.setPasswordHash(PASSWORD_HASH);
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        return user;
    }

    public static User admin() {
        User user = new User();
        user.setId(ADMIN_ID);
        user.setEmail(ADMIN_EMAIL);
        user.setPasswordHash(PASSWORD_HASH);
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        return user;
    }

    public static User disabledStudent() {
        User user = student();
        user.setEnabled(false);
        return user;
    }

    public static StudentProfile studentProfile() {
        StudentProfile profile = new StudentProfile();
        profile.setUser(student());
        profile.setUserId(STUDENT_ID);
        profile.setFullName("Test Student");
        return profile;
    }

    public static AdminProfile adminProfile() {
        AdminProfile profile = new AdminProfile();
        profile.setUser(admin());
        profile.setUserId(ADMIN_ID);
        profile.setDisplayName("Test Admin");
        return profile;
    }
}