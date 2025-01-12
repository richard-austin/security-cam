package com.securitycam.model;


import com.securitycam.security.MyUserDetails;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.HashSet;

@Entity
@Table(name = "user_account")
public class User implements MyUserDetails {

//    @Id
//    @Column(unique = true, nullable = false)
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    private Long id;

    @Email
    @Column(unique = true)
    private String email;

    private boolean cloudAccount = false;
    private String header = "";

    @Id
    @Column(length = 40)
    @Size(min=5, max = 40)
    @NotBlank
    @NotNull
    private String username;

    @Column(length = 60)
    @Size(min=6, max = 60)
    @NotBlank
    @NotNull
    private String password;

    private boolean enabled;
    private boolean credentialsNonExpired;

    private String secret;

    //

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "username"), inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Collection<Role> roles;

    public User(String username, String password, boolean enabled, boolean credentialsNonExpired, boolean cloudAccount, String header, String secret) {
        super();
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.credentialsNonExpired = credentialsNonExpired;
        this.cloudAccount = cloudAccount;
        this.header = header;
        this.secret = secret;
    }

    public User() {
        super();
        this.secret = Base32.random();
        this.enabled = false;
        this.credentialsNonExpired = false;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String username) {
        this.email = username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getUsername() {return username;}

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public boolean getCloudAccount() {
        return cloudAccount;
    }

    public void setCloudAccount(final boolean cloudAccount) {
        this.cloudAccount = cloudAccount;
    }

    public String getHeader() {
        return header;
    }

    @Override
    public boolean getEnabled() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    public void setCredentialsNonExpired(final boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public void setHeader(final String header) {
        this.header = header;
    }

    public Collection<GrantedAuthority> getAuthorities() {
        final Collection<GrantedAuthority> retVal = new HashSet<>();
        roles.forEach(role ->
                retVal.add(new SimpleGrantedAuthority(role.getName())));
        return retVal;
    }

    @Override
    public Collection<Role> getRoles() {
        return roles;
    }

    public void setRoles(final Collection<Role> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((getEmail() == null) ? 0 : getEmail().hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User user = (User) obj;
        if (!getEmail().equals(user.getEmail())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "User [username=" +
                username +
                ", email=" + email +
                ", enabled=" + enabled +
                ", secret=" + secret +
                ", roles=" + roles +
                "]";
   }
}
