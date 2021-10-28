package security.cam

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.apache.commons.io.IOUtils
import org.grails.core.exceptions.GrailsException
import org.grails.web.json.JSONObject
import org.grails.web.json.parser.JSONParser
import security.cam.commands.UpdateCamerasCommand
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

@Transactional
class CamService {
    GrailsApplication grailsApplication
    LogService logService
    ConfigurationUpdateService configurationUpdateService
    Sc_processesService sc_processesService

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
                fis = new FileInputStream("/home/security-cam/cameras.json")
            else
                throw new Exception('Unknown environment, expecting production or development')

            String data = IOUtils.toString(fis, "UTF-8")
            InputStream is = new ByteArrayInputStream(data.getBytes())
            JSONParser parser = new JSONParser(is)

            Object obj = parser.parse()
            result.setResponseObject(obj)
        }
        catch (Throwable ex) {
            logService.cam.error "Exception in getCameras -> parse: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = "The config file is corrupt, empty or does not exist. You can create a new config file using the Configure Camera Setup option under the General menu.   ....   " + ex.getMessage()
        }

        return result
    }

    def updateCameras(UpdateCamerasCommand cmd)
    {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try
        {
            Gson gson = new GsonBuilder().setPrettyPrinting().create()
            JsonElement je = JsonParser.parseString(cmd.camerasJSON)
            String prettyJsonString = gson.toJson(je)

            String fileName
            if(Environment.current.name == 'development')
                fileName = "../xtrn-scripts-and-config/cameras_dev.json"
            else if(Environment.current.name == 'production')
                fileName = "/home/security-cam/cameras.json"
            else
                throw new Exception('Unknown environment, expecting production or development')

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(prettyJsonString)

            writer.close()

            sc_processesService.stopProcesses()
            Thread.sleep(500)
            configurationUpdateService.generateConfigs()
            sc_processesService.startProcesses()

            JSONObject obj = new JSONObject(prettyJsonString)
            result.setResponseObject(obj)
        }
        catch(Exception ex)
        {
            logService.cam.error "Exception in updateCameras: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
