package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

@Transactional
class CamService {
    GrailsApplication grailsApplication

    def getCameras() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            response.responseObject = grailsApplication.config.cameras
        }
        catch(Exception ex)
        {
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }
}
