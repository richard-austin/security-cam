package security.cam.commands

import grails.validation.Validateable

class SetWifiStatusCommand implements Validateable{
    String status
    boolean isCloud = true

    static constraints = {
        status(nullable: false, inList: ["on", "off"])
        isCloud(nullable: false, inList:[true, false])
    }
}
