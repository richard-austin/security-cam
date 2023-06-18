package security.cam.commands

import grails.validation.Validateable
import server.Camera
import server.Stream

class StartAudioOutCommand implements Validateable{
    Stream stream

    static constraints = {
        stream(nullable: false, blank: false)
    }
}
