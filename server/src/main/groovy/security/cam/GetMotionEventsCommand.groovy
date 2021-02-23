package security.cam

import grails.core.GrailsApplication
import grails.util.Pair
import grails.validation.Validateable

class GetMotionEventsCommand implements Validateable{
    String cameraName

    GrailsApplication grailsApplication

    static constraints = {
        cameraName(nullable: false, size: 1..50,
                validator: { cameraName, cmd ->
                    def cameraNames = cmd.grailsApplication.config.motion.cameraNames

                    def result = cameraNames.find{
                        it['name'] == cameraName
                    }

                    if(result == null)
                        return "Camera name ${cameraName} not known"
                })
    }
}
