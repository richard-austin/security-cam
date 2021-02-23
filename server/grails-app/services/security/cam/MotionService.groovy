package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class MotionService {
    GrailsApplication grailsApplication

    /**
     * Get the names of the motion event files
     * @param cameraName
     * @return
     */
    def getMotionEvents(String cameraName) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            String motionEventsDirectory = grailsApplication.config.motion.motionEventsDirectory

            File f = new File(motionEventsDirectory)

            result.responseObject = f.list()

            if(result.responseObject == null)
            {
                result.status = PassFail.FAIL
                result.error = "Cannot access motion events"
            }
        }
        catch(Exception ex)
        {
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }
}
