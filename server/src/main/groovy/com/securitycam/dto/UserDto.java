package com.securitycam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserDto {
    @NotNull
    @Size(min = 5, max=30)
    @Pattern(regexp =  "^[a-zA-Z0-9](_(?!(.|_))|.(?!(_|.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$", message = "Incorrect characters for username")
    private String username;

    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9(){\\[1*£$\\\\\\]}=@~?^]{7,31}$", message = "Bad password format")
    private String password;

    @NotNull
    @Size(min = 1)
    private String matchingPassword;

    @NotNull
    @Size(min = 6, message = "{Size.userDto.email}")
    @Email(message = "Incorrect email format")
    private String email;

    private boolean cloudAccount;
    private String header;

    public boolean getCloudAccount() {
        return cloudAccount;
    }
    public String getPassword() {
        return password;
    }
    public String getMatchingPassword() {
        return matchingPassword;
    }

    public String getEmail() {
        return email;
    }
    public String getHeader() {
        return header;
    }

    public void setCloudAccount(boolean cloudAccount) {
        this.cloudAccount = cloudAccount;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setMatchingPassword(String matchingPassword) {
        this.matchingPassword = matchingPassword;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setHeader(String header) {
        this.header = header;
    }

    private Integer role;

    public @NotNull @Size(min = 5, max = 30) String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "UserDto [firstName=" +
                ", email=" +
                email +
                ", role=" +
                role + "]";
    }

}
