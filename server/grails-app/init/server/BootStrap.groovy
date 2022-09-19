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
        List<String> authorities = ['ROLE_CLIENT', 'ROLE_GUEST', 'ROLE_CLOUD']
        authorities.each { authority ->
            if ( !roleService.findByAuthority(authority) ) {
                roleService.save(authority)
            }
        }
        if ( !userService.findByUsername('cloud') ) {
            User u = new User(username: 'cloud', password: 'DrN3yuFAtSsK2w7AtTf66FFRVveBwtjU', cloudAccount: true, header: "7yk=zJu+@77x@MTJG2HD*YLJgvBthkW!")
            u = userService.save(u)
            userRoleService.save(u, roleService.findByAuthority('ROLE_CLOUD'))
        }

        if ( !userService.findByUsername('guest') ) {
            User u = new User(username: 'guest', password: 'guest', cloudAccount: false, enabled: false, passwordExpired: true)
            u = userService.save(u)
            userRoleService.save(u, roleService.findByAuthority('ROLE_GUEST'))
        }

        sc_processesService.startProcesses()

        // Start CloudProxy if enabled in the config or if there is no local web account other than guest on the NVR
        if(grailsApplication.config.cloudProxy.enabled || User.all.find{it.username != 'guest' && !it.cloudAccount} == null)
            cloudProxyService.start()
    }
    def destroy = {
        sc_processesService.stopProcesses()
        if (grailsApplication.config.cloudProxy.enabled)
            cloudProxyService.stop()
    }
}
