package security.cam

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import security.cam.commands.ResetPasswordCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import java.security.Principal

@Transactional
class UserAdminService {
    SpringSecurityService springSecurityService
    LogService logService

    ObjectCommandResponse resetPassword(ResetPasswordCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder()

            Principal principal = springSecurityService.getPrincipal() as Principal
            String userName = principal.getName()

            User user = User.findByUsername(userName)
            user.setPassword(cmd.getNewPassword())
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
