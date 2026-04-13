package com.example.backend.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String firstName;

    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    private String lastName;

    private String roleId;
}
