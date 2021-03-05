package security.cam.commands

import grails.validation.Validateable
import security.cam.MotionService

class GetOffsetForEpochCommand implements Validateable{
    Long epoch
    String motionName

    MotionService motionService

    static constraints = {
        epoch(nullable: false,
                validator: { epoch, cmd ->
                    if( epoch < 100000 || epoch > Integer.MAX_VALUE)
                        return "Epoch value is invalid"
                    return
                })
        motionName(nullable: false,
        validator: {String motionName, cmd ->
            if(!cmd.motionService.epochToOffsetHasEntryFor(motionName))
                return "The motion events name ${motionName} is not in the map"
        })
    }
}
