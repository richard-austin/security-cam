package server

import grails.core.GrailsApplication
import security.cam.CloudProxyService
import security.cam.RoleService
import security.cam.Sc_processesService
import security.cam.User
import security.cam.UserRoleService
import security.cam.UserService

class BootStrap {

    UserService userService
    RoleService roleService
    UserRoleService userRoleService
    Sc_processesService sc_processesService
    CloudProxyService cloudProxyService
    GrailsApplication grailsApplication

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

        sc_processesService.startProcesses()
        if(grailsApplication.config.cloudProxy.enabled)
            cloudProxyService.start()
    }
    def destroy = {
//   //     sc_processesService.stopProcesses()
    }
}
