package com.example.backend.services;

import com.example.backend.dtos.request.LoginRequest;
import com.example.backend.dtos.response.LoginResponse;
import com.example.backend.entities.User;
import com.example.backend.exceptions.ApiException;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ApiException("Credenciales inválidas", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getRole().getName());
    }
}
