package security.cam

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import security.cam.commands.ResetPasswordCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional()
class UserAdminService {
    SpringSecurityService springSecurityService
    LogService logService

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
}
