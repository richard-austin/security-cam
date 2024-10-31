package com.securitycam.services

import com.securitycam.commands.ChangeEmailCommand
import com.securitycam.commands.ResetPasswordCommand
import com.securitycam.commands.SetupGuestAccountCommand
import com.securitycam.dao.UserRepository
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserAdminService {
    @Autowired
    LogService logService

    @Autowired
    UserRepository userRepository

    @Autowired
    PasswordEncoder passwordEncoder
    @Autowired
    SimpMessagingTemplate brokerMessagingTemplate

    final String logoff = new JSONObject()
            .put("message", "logoff")
            .toString()


    ObjectCommandResponse resetPassword(ResetPasswordCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth.getPrincipal()
            if (principal) {
                String userName = auth.getName()

                User user = userRepository.findByUsername(userName)
                user.setPassword(passwordEncoder.encode(cmd.getNewPassword()))
                userRepository.save(user)
            }
            else
                throw new Exception("Could not get principal for this user")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in resetPassword: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

//    def resetPasswordFromLink(ResetPasswordFromLinkCommand cmd) {
//        ObjectCommandResponse result = new ObjectCommandResponse()
//        try {
//            if (passwordResetParameterMap.containsKey(cmd.resetKey)) {
//                clearPasswordResetKeyMapAndTimer()
//                String userName
//                def users = User.findAll()
//                users.forEach { user ->
//                    def auths = user.getAuthorities()
//                    auths.forEach { role ->
//                        if (role.authority == 'ROLE_CLIENT') {
//                            user.setPassword(cmd.getNewPassword())
//                            user.save()
//                            return result
//                        }
//                    }
//                }
//            } else {
//                logService.cam.error("Invalid password reset key")
//                result.status = PassFail.FAIL
//                result.error = "Invalid password reset key"
//            }
//        }
//        catch (Exception ex) {
//            logService.cam.error("Exception in resetPassword: " + ex.getCause() + ' ' + ex.getMessage())
//            result.status = PassFail.FAIL
//            result.error = ex.getMessage()
//        }
//
//        return result
//    }
    ObjectCommandResponse isGuest() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            boolean isGuest = false
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth.getPrincipal()
            if (principal) {
                String userName = auth.getName()

                User user = userRepository.findByUsername(userName)

                if (user) {
                    Collection<GrantedAuthority> auths = user.getAuthorities()

                    auths.forEach { role ->
                        if (role.getAuthority() == 'ROLE_GUEST')
                            isGuest = true
                    }
                }
            }
            result.responseObject = [guestAccount: isGuest]
        }
        catch (Exception ex) {
            logService.cam.error("Exception in isGuest: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse guestAccountEnabled() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User guest = userRepository.findByUsernameAndCloudAccount("guest", false)
            result.responseObject = [enabled: guest != null && guest.enabled]
        }
        catch (Exception ex) {
            logService.cam.error("Exception in guestAccountEnabled: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
    ObjectCommandResponse getEmail() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth.getPrincipal()
            if(principal) {
                String userName = auth.getName()
                User user = userRepository.findByUsername(userName)
                result.responseObject = [email: user.getEmail()]
            }
            else
                throw new Exception("Could not get principal for this user")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getEmail: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse setupGuestAccount(SetupGuestAccountCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User u = userRepository.findByUsernameAndCloudAccount('guest', false)
            u.enabled = cmd.enabled
            if (cmd.password != "" && cmd.password != null) {
                u.setPassword(passwordEncoder.encode(cmd.password))

            }
            u.credentialsNonExpired = true
            userRepository.save(u)

            // Kick off any guest users who are logged in
            // TODO: This will also kick off non-guest users, it should be changed to specify guest users only
            if (!cmd.enabled)
                brokerMessagingTemplate.convertAndSend("/topic/logoff", logoff)
        }
        catch (Exception ex) {
            logService.cam.error("Exception in setupGuestAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }


    ObjectCommandResponse changeEmail(ChangeEmailCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth.getPrincipal()
            if(principal) {
                String userName = auth.getName()
                User user = userRepository.findByUsername(userName)
                user.setEmail(cmd.getNewEmail())
                userRepository.save(user)
            }
            else
                throw new Exception("Could not get principal for this user")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in changeEmail: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
