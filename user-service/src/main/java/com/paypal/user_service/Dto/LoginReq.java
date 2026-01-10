package com.paypal.user_service.Dto;

public class LoginReq {
    private String email;
    private String password;

    public LoginReq() {}

    public LoginReq(String username, String password) {
        this.email = username;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setUsername(String username) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

