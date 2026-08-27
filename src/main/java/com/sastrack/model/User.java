package com.sastrack.model;

public class User {
    private final String username;
    private final String password;
    private final String role; // "SAS Head" (admin) or "SAS Staff"

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "SAS Head".equalsIgnoreCase(role); }
}
