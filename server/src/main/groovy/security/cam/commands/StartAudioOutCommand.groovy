package security.cam.commands

import grails.validation.Validateable
import server.Camera
import server.Stream

class StartAudioOutCommand implements Validateable{
    Camera cam
    String netcam_uri

    static constraints = {
        cam(nullable: false, blank: false)
        netcam_uri(nullable: false, blank: false)
    }
}
