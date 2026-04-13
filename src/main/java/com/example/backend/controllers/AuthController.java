package com.example.backend.controllers;

import com.example.backend.dtos.request.LoginRequest;
import com.example.backend.dtos.response.ApiResponse;
import com.example.backend.dtos.response.LoginResponse;
import com.example.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(data, "Login exitoso"));
    }
}
