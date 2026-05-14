package com.securitycam.security;

import com.securitycam.dao.UserRepository;
import com.securitycam.model.User;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("userDetailsService")
public class MyUserDetailsService implements UserDetailsService {

    @Autowired  // Tried the injection in constructor alternative to @Autowired (which the warning says is not recommended) but it results in userRepository being null
    private UserRepository userRepository;


    // API
    @Override
    public @NonNull MyUserDetails loadUserByUsername(final @NonNull String username) throws UsernameNotFoundException {
            final User user = userRepository.findByUsername(username);

            if (user == null) {
                throw new UsernameNotFoundException("No user found with username: " + username);
            }
           return user;
    }

    // UTIL
    private List<GrantedAuthority> getGrantedAuthorities(final List<String> privileges) {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        for (final String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }
}
