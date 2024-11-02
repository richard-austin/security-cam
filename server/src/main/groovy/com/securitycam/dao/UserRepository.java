package com.securitycam.dao;

import com.securitycam.model.Role;
import com.securitycam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
    User findByUsernameAndCloudAccount (String username, boolean cloudAccount);
    User findByUsernameNotAndCloudAccount(String username, boolean cloudAccount);
    User findByRoles(Collection<Role> roles);

    @Override
    void delete(User user);

}
