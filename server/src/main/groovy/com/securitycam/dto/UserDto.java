package com.securitycam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class UserDto {
    @NotNull
    @Size(min = 5, max=30)
    @Pattern(regexp =  "^[a-zA-Z0-9](_(?!(.|_))|.(?!(_|.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$", message = "Incorrect characters for username")
    private String username;

    @Setter
    @Getter
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9(){\\[1*£$\\\\\\]}=@~?^]{7,31}$", message = "Bad password format")
    private String password;

    @Setter
    @Getter
    @NotNull
    @Size(min = 1)
    private String matchingPassword;

    @Setter
    @Getter
    private boolean credentialsNonExpired;

    @Setter
    @Getter
    @NotNull(message = "email is not nullable")
    @Size(min = 6, message = "{Size.userDto.email}")
    @Email(message = "Incorrect email format")
    private String email;

    @Setter
    private boolean cloudAccount;
    @Setter
    @Getter
    private String header;

    public boolean getCloudAccount() {
        return cloudAccount;
    }

    @Getter
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
