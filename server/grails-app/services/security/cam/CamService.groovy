package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.apache.commons.io.IOUtils
import org.grails.web.json.JSONObject
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

@Transactional
class CamService {
    GrailsApplication grailsApplication
    LogService logService

    /**
     * getCameras: Get all cameras defined in the application.yml file
     * @return
     */
    def getCameras() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            FileInputStream fis

            if(Environment.current.name == 'development')
                fis = new FileInputStream("/home/security-cam/cameras_dev.json");
            else if(Environment.current.name == 'production')
                fis = new FileInputStream("/home/security-cam/cameras.json")
            else
                throw new Exception('Unknown environment, expecting production or development')

            String data = IOUtils.toString(fis, "UTF-8");
            JSONObject obj = new JSONObject(data)

            result.setResponseObject(obj)
        }
        catch (Exception ex) {
            logService.cam.error "Exception in parse: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }
}
