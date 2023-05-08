package security.cam.commands

import grails.validation.Validateable

class StartAudioOutCommand implements Validateable{
    String camera

    static constraints = {
        camera(nullable: false, blank: false)
    }
}
