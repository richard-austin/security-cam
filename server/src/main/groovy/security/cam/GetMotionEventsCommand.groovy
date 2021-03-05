package security.cam

import grails.validation.Validateable

class GetMotionEventsCommand implements Validateable{
    def camera

    static constraints = {
        camera(nullable: false,
            validator: { camera, cmd ->
                if( camera.name == null || camera.name == "")
                    return "Camera name is null"
                return
            })
        }
}
