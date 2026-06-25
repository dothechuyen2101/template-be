package com.example.template.modules.role.repository;

import com.example.template.modules.role.entity.Role;
import com.example.template.modules.role.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
