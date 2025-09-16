package com.ido.idoprojectapp;

public class User {
    private String username;
    private String password;
    private String email;

    public User(String username,  String email, String password) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    public User(String username,  String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
