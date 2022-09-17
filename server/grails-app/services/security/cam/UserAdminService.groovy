package security.cam

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import security.cam.commands.ChangeEmailCommand
import security.cam.commands.CreateAccountCommand
import security.cam.commands.ResetPasswordCommand
import security.cam.commands.SetupGuestAccountCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional()
class UserAdminService {
    SpringSecurityService springSecurityService
    LogService logService

    UserService userService
    UserRoleService userRoleService
    RoleService roleService

    ObjectCommandResponse resetPassword(ResetPasswordCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def principal = springSecurityService.getPrincipal()
            String userName = principal.getUsername()

            User user = User.findByUsername(userName)
            user.setPassword(cmd.getNewPassword())
            user.save()
        }
        catch (Exception ex) {
            logService.cam.error("Exception in resetPassword: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    /**
     * createAccount: This is used to create a local web account on the NVR which can be logged into directly, rather
     *                than via the Cloud service.
     * @param cmd :     String username
     *                 String password
     *                 String confirmPassword
     *                 String email
     *                 String confirmEmail
     *
     * @return: Success/error status
     */
    ObjectCommandResponse createAccount(CreateAccountCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User u = new User(username: cmd.username, password: cmd.password, email: cmd.email, cloudAccount: false, header: null)
            u = userService.save(u)
            userRoleService.save(u, roleService.findByAuthority('ROLE_CLIENT'))

        }
        catch (Exception ex) {
            logService.cam.error("Exception in createAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    /**
     * removeAccount: Remove the local NVR direct web access account
     * @return Success/error status
     */
    ObjectCommandResponse removeAccount() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            User user = User.all.find { it.username != 'guest' && !it.cloudAccount }
            if (user != null) {
                result.responseObject = [username: user.getUsername()]
                UserRole userRole = UserRole.findByUser(user)
                userRole.delete(flush: true)
                user.delete(flush: true)

                // Disable the guest account
                user = User.findByUsername('guest')
                user.setEnabled(false)
                userService.save(user)
            } else
                throw new Exception("There is no local web account present on this NVR")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in removeAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse hasLocalAccount() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            result.responseObject = User.all.find { it.username != 'guest' && !it.cloudAccount } != null
        }
        catch (Exception ex) {
            logService.cam.error("Exception in hasLocalAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse setupGuestAccount(SetupGuestAccountCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User u = User.all.find { it.username == 'guest' && !it.cloudAccount }
            u.enabled = cmd.enabled
            if (cmd.password != "" && cmd.password != null)
                u.setPassword(cmd.password)

            u.passwordExpired = false
            userService.save(u)
        }
        catch (Exception ex) {
            logService.cam.error("Exception in setupGuestAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse getEmail() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def principal = springSecurityService.getPrincipal()
            String userName = principal.getUsername()

            User user = User.findByUsername(userName)

            result.responseObject = [email: user.getEmail()]
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getEmail: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse changeEmail(ChangeEmailCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def principal = springSecurityService.getPrincipal()
            String userName = principal.getUsername()

            User user = User.findByUsername(userName)
            user.setEmail(cmd.getNewEmail())
            user.save()
        }
        catch (Exception ex) {
            logService.cam.error("Exception in changeEmail: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse isGuest() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def principal = springSecurityService.getPrincipal()
            String userName = principal.getUsername()

            User user = User.findByUsername(userName)
            result.responseObject = [guestAccount: user.username == "guest" && !user.cloudAccount]
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in isGuest: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse guestAccountEnabled()
    {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User guest = User.all.find{it.username == "guest" && !it.cloudAccount}
            result.responseObject = [enabled: guest != null && guest.enabled]
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in guestAccountEnabled: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
