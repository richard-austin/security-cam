package security.cam.commands

import grails.validation.Validateable
import security.cam.MotionService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class GetOffsetForEpochCommand implements Validateable{
    Long epoch
    String motionName

    MotionService motionService

    static constraints = {
        epoch(nullable: false,
                validator: { epoch, cmd ->
                    if(epoch < 100000 || epoch > Integer.MAX_VALUE)
                        return "Epoch value is invalid"
                    return
                })
        motionName(nullable: false,
        validator: {String motionName, cmd ->
            ObjectCommandResponse resp = cmd.motionService.epochToOffsetHasEntryFor(motionName)
            if(resp.status == PassFail.PASS)
            {
                if(!resp.responseObject)
                    return "The motion events name ${motionName} is not in the map"
            }
            else
                return resp.error
        })
    }
}
