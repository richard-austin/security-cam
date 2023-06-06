package security.cam.commands

import grails.validation.Validateable

class CloseClientsCommand implements Validateable{
    String accessToken
    static constraints = {
        accessToken(nullable: false, blank: false)
    }
}
