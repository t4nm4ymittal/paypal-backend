package com.paypal.user_service.Dto;

public class JwtReq {
    private String token;

    public JwtReq() {}

    public JwtReq(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

