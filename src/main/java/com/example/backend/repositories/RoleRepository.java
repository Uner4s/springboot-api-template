package com.example.backend.repositories;

import com.example.backend.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {

    List<Role> findAllByIsDeletedFalse();

    Optional<Role> findByIdAndIsDeletedFalse(String id);

    Optional<Role> findByNameAndIsDeletedFalse(String name);

    boolean existsByName(String name);
}
