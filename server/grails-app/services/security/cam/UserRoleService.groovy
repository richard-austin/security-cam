package security.cam

import com.securitycam.Role
import com.securitycam.User
import com.securitycam.UserRole
import grails.gorm.services.Service

@Service(UserRole)
interface UserRoleService {

    UserRole save(User user, Role role)
}