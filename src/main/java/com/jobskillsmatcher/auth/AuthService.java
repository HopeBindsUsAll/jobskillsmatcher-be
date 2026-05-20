package com.jobskillsmatcher.auth;

import com.jobskillsmatcher.auth.model.AuthToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.text.ParseException;

public interface AuthService {

    void jwtFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException;

    void jwtFilter(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException;

    void authenticate(HttpServletRequest request, HttpServletResponse response, Authentication authentication);

    void failure(HttpServletRequest request, HttpServletResponse response, RuntimeException exception);

    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication);

    AuthToken refreshToken(String refreshToken, String accessToken) throws ParseException;

    AuthToken register(String email, String password, String fullName);

    AuthToken login(String email, String password);

    void handle(HttpServletResponse response, Object payload);
}
