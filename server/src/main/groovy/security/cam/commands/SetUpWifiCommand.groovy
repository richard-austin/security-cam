package security.cam.commands

import grails.validation.Validateable

class SetUpWifiCommand implements Validateable {
    String ssid
    String password
    boolean isCloud = true

    static constraints = {
        ssid(nullable: false, blank: false)
        password(nullable: true, blank: false)
        isCloud(nullable: false, inList: [true, false])
    }
}
