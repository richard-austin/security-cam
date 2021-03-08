package security.cam.commands

import grails.validation.Validateable
import server.Camera

class GetMotionEventsCommand implements Validateable{
    Camera camera

    static constraints = {
        camera(nullable: false,
            validator: { camera, cmd ->
                if( camera.name == null || camera.name == "")
                    return "Camera name is null"
                return
            })
        }
}
