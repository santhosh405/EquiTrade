package com.example.demo.dto;

public class JwtResponse {

    private String token;
    private String message;

    public JwtResponse() {
    }

    public JwtResponse(String token, String message) {
        this.token = token;
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}