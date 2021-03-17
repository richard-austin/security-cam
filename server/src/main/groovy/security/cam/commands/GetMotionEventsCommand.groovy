package security.cam.commands

import grails.validation.Validateable
import server.Camera

class GetMotionEventsCommand{
    Camera camera

    static constraints = {
        camera(nullable: false,
            validator: { camera, cmd ->
                if( camera.name == null || camera.name == "")
                    return "No camera was specified to find motion events for"
                return
            })
        }
}
