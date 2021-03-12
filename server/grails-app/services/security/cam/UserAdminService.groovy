package security.cam

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class UserAdminService {
    SpringSecurityService springSecurityService

    ObjectCommandResponse resetPassword(ResetPasswordCommand) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder()
        ObjectCommandResponse response = new ObjectCommandResponse()


        Object principal = springSecurityService.getPrincipal()
    }
}
