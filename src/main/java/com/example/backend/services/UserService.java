package com.example.backend.services;

import com.example.backend.dtos.request.CreateUserRequest;
import com.example.backend.dtos.request.UpdateUserRequest;
import com.example.backend.dtos.response.GetUserResponse;
import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import com.example.backend.exceptions.ApiException;
import com.example.backend.mappers.UserMapper;
import com.example.backend.repositories.RoleRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<GetUserResponse> findAll() {
        return userRepository.findAllByIsDeletedFalse()
                .stream()
                .map(userMapper::toGetResponse)
                .toList();
    }

    public GetUserResponse findById(String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND));
        return userMapper.toGetResponse(user);
    }

    @Transactional
    public GetUserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("El email ya está registrado", HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository.findByIdAndIsDeletedFalse(request.getRoleId())
                .orElseThrow(() -> new ApiException("Rol no encontrado", HttpStatus.NOT_FOUND));

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        return userMapper.toGetResponse(userRepository.save(user));
    }

    @Transactional
    public GetUserResponse update(String id, UpdateUserRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());

        if (request.getRoleId() != null) {
            Role role = roleRepository.findByIdAndIsDeletedFalse(request.getRoleId())
                    .orElseThrow(() -> new ApiException("Rol no encontrado", HttpStatus.NOT_FOUND));
            user.setRole(role);
        }

        return userMapper.toGetResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND));
        user.setIsDeleted(true);
        userRepository.save(user);
    }
}
