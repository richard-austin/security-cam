package security.cam

import com.securitycam.Role
import com.securitycam.User
import grails.gorm.transactions.Transactional

import static com.securitycam.UserRole.*

class BootStrap {
    UserService userService
    RoleService roleService
    UserRoleService userRoleService


    def init = { servletContext ->
        List<String> authorities = ['ROLE_CLIENT']
        authorities.each { authority ->
            if ( !roleService.findByAuthority(authority) ) {
                roleService.save(authority)
            }
        }
        if ( !userService.findByUsername('admin') ) {
            User u = new User(username: 'admin', password: 'elementary')
            u = userService.save(u)
            userRoleService.save(u, roleService.findByAuthority('ROLE_CLIENT'))
        }
    }
    def destroy = {
    }
}
