package com.securitycam.dao;

import com.securitycam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
    User findByUsernameAndCloudAccount (String username, boolean cloudAccount);
    User findByUsernameNotAndCloudAccount(String username, boolean cloudAccount);
    @Override
    void delete(User user);

}
