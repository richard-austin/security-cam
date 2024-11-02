package com.securitycam.commands

class CreateOrUpdateAccountCommand {
    String username
    String password
    String confirmPassword
    String email
    String confirmEmail
    boolean updateExisting = false
}
