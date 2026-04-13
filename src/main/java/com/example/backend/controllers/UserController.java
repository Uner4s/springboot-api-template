package com.example.backend.controllers;

import com.example.backend.dtos.request.CreateUserRequest;
import com.example.backend.dtos.request.UpdateUserRequest;
import com.example.backend.dtos.response.ApiResponse;
import com.example.backend.dtos.response.GetUserResponse;
import com.example.backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'READ_USERS')")
    public ResponseEntity<ApiResponse<List<GetUserResponse>>> getAll() {
        List<GetUserResponse> data = userService.findAll();
        return ResponseEntity.ok(ApiResponse.success(data, "Usuarios obtenidos"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'READ_USERS')")
    public ResponseEntity<ApiResponse<GetUserResponse>> getById(@PathVariable String id) {
        GetUserResponse data = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Usuario obtenido"));
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<GetUserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        GetUserResponse data = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, "Usuario creado"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<GetUserResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        GetUserResponse data = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Usuario actualizado"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado"));
    }
}
