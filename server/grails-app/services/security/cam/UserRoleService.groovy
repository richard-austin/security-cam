package security.cam


import grails.gorm.services.Service
import security.cam.Role
import security.cam.User
import security.cam.UserRole

@Service(UserRole)
interface UserRoleService {

    UserRole save(User user, Role role)
}