package security.cam.commands

import grails.core.GrailsApplication
import grails.validation.Validateable
import org.apache.commons.lang.StringUtils
import security.cam.UtilsService
import security.cam.interfaceobjects.ConnectionDetails

class SetUpWifiCommand implements Validateable {
    String ssid
    String password

    static constraints = {
        ssid(nullable: false, blank: false)
        password(nullable: false, blank: false)
    }
}
