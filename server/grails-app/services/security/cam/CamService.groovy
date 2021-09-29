package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.apache.commons.io.IOUtils
import org.apache.tomcat.util.json.JSONParser
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
                fis = new FileInputStream("../xtrn-scripts-and-config/cameras_dev.json")
            else if(Environment.current.name == 'production')
                fis = new FileInputStream("/etc/security-cam/cameras.json")
            else
                throw new Exception('Unknown environment, expecting production or development')

            String data = IOUtils.toString(fis, "UTF-8")
            JSONParser parser = new JSONParser(data)
            Object obj = parser.parse()
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
