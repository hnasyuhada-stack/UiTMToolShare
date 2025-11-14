package com.example.uitmtoolshare;

public class User {
    public String email, username, phone;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String email, String username, String phone) {
        this.email = email;
        this.username = username;
        this.phone = phone;
    }
}
