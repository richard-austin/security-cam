package security.cam

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import security.cam.commands.CreateAccountCommand
import security.cam.commands.ResetPasswordCommand
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
        catch(Exception ex)
        {
            logService.cam.error("Exception in resetPassword: "+ex.getCause()+ ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

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
}
