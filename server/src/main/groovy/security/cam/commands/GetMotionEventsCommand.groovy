package security.cam.commands

import grails.validation.Validateable
import server.Camera
import server.Stream

class GetMotionEventsCommand implements Validateable {
    Stream stream
    Camera cam

    static constraints = {
        cam(nullable: false,
            validator: { cam, cmd ->
                if( cam.name == null || cam.name == "")
                    return "No cam was specified to find motion events for"
                return
            })
        stream(nullable: false)
    }
}
