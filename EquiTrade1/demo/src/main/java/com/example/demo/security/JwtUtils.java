package com.example.demo.security;

import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    public String generateToken(String email) {
        return "Token-" + email;
    }

}