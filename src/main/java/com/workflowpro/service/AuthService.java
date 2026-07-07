package com.workflowpro.service;

import com.workflowpro.dto.AuthDtos.AuthResponse;
import com.workflowpro.dto.AuthDtos.LoginRequest;
import com.workflowpro.dto.AuthDtos.RegisterRequest;
import com.workflowpro.dto.UserResponse;
import com.workflowpro.entity.Role;
import com.workflowpro.entity.User;
import com.workflowpro.exception.ApiException;
import com.workflowpro.repository.UserRepository;
import com.workflowpro.security.CustomUserDetailsService;
import com.workflowpro.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user = userRepository.save(user);

        UserDetails details = userDetailsService.loadUserByUsername(email);
        return new AuthResponse(jwtService.generateToken(details), "Bearer", UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        UserDetails details = userDetailsService.loadUserByUsername(email);
        return new AuthResponse(jwtService.generateToken(details), "Bearer", UserResponse.from(user));
    }
}
