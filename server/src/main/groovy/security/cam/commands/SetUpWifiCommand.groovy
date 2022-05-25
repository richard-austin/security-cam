package security.cam.commands

import grails.validation.Validateable

class SetUpWifiCommand implements Validateable {
    String ssid
    String password

    static constraints = {
        ssid(nullable: false, blank: false)
        password(nullable: true, blank: false)
    }
}
