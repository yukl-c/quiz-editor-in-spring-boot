package com.app.quiz.model;

public class User {
    private String username;
    private String email;
    private String password;
    private String role;

    public User(String username, String email, String password, String role) {
    // public User(String username, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return ("User name: " + this.username 
                + "\nEmail: " + this.email
                + "\nrole: " + this.role);
    }
}