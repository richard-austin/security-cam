package server

import grails.plugin.springsecurity.annotation.Secured

class UserController {

    @Secured(['ROLE_CLIENT'])
    def changePassword()
    {

    }
}
