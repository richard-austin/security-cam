package security.cam.commands

import grails.validation.Validateable

class SetWifiStatusCommand implements Validateable{
    String status

    static constraints = {
        status(nullable: false, inList: ["on", "off"])
    }
}
