package security.cam.commands

import grails.validation.Validateable
import server.Camera

class StartAudioOutCommand implements Validateable{
    Camera camera

    static constraints = {
        camera(nullable: false, blank: false)
    }
}
