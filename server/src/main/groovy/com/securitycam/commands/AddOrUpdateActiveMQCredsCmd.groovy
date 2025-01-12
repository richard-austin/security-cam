package com.securitycam.commands

class AddOrUpdateActiveMQCredsCmd extends CheckNotGuestCommand {
    String username
    String password
    String confirmPassword
    String mqHost
}
