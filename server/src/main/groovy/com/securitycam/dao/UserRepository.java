package com.securitycam.dao;

import com.securitycam.model.User;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
    User findByUsernameAndCloudAccount (String username, boolean cloudAccount);
    User findByUsernameNotAndCloudAccount(String username, boolean cloudAccount);
    User findByCloudAccountAndUsernameNot(boolean cloudAccount, String username);
    @Override
    void delete(@NonNull User user);

}
