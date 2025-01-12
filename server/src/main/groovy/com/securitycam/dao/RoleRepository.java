package com.securitycam.dao;


import com.securitycam.model.Role;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String name);
    @NotNull Optional<Role> findById(@NotNull Long id);
    @Override
    void delete(@NotNull Role role);

}
