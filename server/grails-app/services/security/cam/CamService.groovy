package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

@Transactional
class CamService {
    GrailsApplication grailsApplication
    LogService logService

    def getCameras() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            response.responseObject = grailsApplication.config.cameras
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in getCameras: "+ex.getMessage())
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }
}
