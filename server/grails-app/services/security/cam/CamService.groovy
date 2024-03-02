package security.cam

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.apache.commons.io.IOUtils
import org.grails.web.json.JSONObject
import security.cam.commands.SetOnvifCredentialsCommand
import security.cam.commands.UpdateCamerasCommand
import security.cam.commands.UploadMaskFileCommand
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail
import server.Camera
import server.Stream

@Transactional
class CamService {
    GrailsApplication grailsApplication
    LogService logService
    ConfigurationUpdateService configurationUpdateService
    Sc_processesService sc_processesService
    OnvifService onvifService

    /**
     * getCameras: Get all cameras defined in the application.yml file
     * @return
     */
    def getCameras() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            FileInputStream fis

            fis = new FileInputStream("${grailsApplication.config.camerasHomeDirectory}/cameras.json")
            String data = IOUtils.toString(fis, "UTF-8")
            Gson gson2 = new Gson()
            Object obj = gson2.fromJson(data, Object.class)

            result.setResponseObject(obj)
        }
        catch (Throwable ex) {
            logService.cam.error "Exception in getCameras -> parse: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = "The config file is corrupt, empty or does not exist. You can create a new config file using the Cameras Configuration option under the General menu.   ....   " + ex.getMessage()
        }

        return result
    }

    static private def removeUnusedMaskFiles(LinkedTreeMap<String, Camera> jsonObj, GrailsApplication grailsApplication) {
        Set<String> mask_files = new HashSet<String>()

        // Make a set of file names which are in use
        for (Map.Entry<String, Camera> cam : jsonObj.entrySet())
            for (Map.Entry<String, Stream> stream : cam.value.streams)
                if (stream.value.motion.enabled && stream.value.motion.mask_file != "")
                    mask_files.add(stream.value.motion.mask_file)

        // Get the .pgm files in the motion directory
        File directory = new File("${grailsApplication.config.camerasHomeDirectory}/motion")
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pgm")
            }
        })

        // Remove .pgm files not in the data set
        for (File file : files) {
            if (!mask_files.contains(file.name))
                file.delete()
        }
    }

    def updateCameras(UpdateCamerasCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create()
            JsonElement je = JsonParser.parseString(cmd.camerasJSON)
            String prettyJsonString = gson.toJson(je)

            String fileName
            fileName = "${grailsApplication.config.camerasHomeDirectory}/cameras.json"

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(prettyJsonString)

            writer.close()

            ObjectCommandResponse stopResult = sc_processesService.stopProcesses()
            result = configurationUpdateService.generateConfigs()
            ObjectCommandResponse startResult = sc_processesService.startProcesses()

            if (result.status !== PassFail.PASS)
                throw new Exception(result.error)

            Gson gson2 = new Gson()
            LinkedTreeMap<String, Camera> obj = gson2.fromJson(prettyJsonString, Object.class) as LinkedTreeMap<String, Camera>

            removeUnusedMaskFiles(obj, grailsApplication)

            if (stopResult.status != PassFail.PASS)
                result = stopResult
            else if (startResult.status != PassFail.PASS)
                result = startResult

            // Populate the Onvif device map to prevent the delay of creating the device at the start of each PTZ operation
            onvifService.populateDeviceMap()
            result.setResponseObject(obj)
        }
        catch (Exception ex) {
            logService.cam.error "Exception in updateCameras: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    def uploadMaskFile(UploadMaskFileCommand cmd) {

        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            File file
            file = new File("${grailsApplication.config.camerasHomeDirectory}/motion/" + cmd.maskFile.originalFilename)
            cmd.maskFile.transferTo(file)
        }
        catch (Exception ex) { // Some other type of exception
            logService.cam.error "uploadMaskFile() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    /**
     * setOnvifCredentials: Set the access credentials required by some cameras to return onvif query results
     * @param cmd : Command object containing the username and password
     * @return: ObjectCommandResponse with success/error state.
     */
    def setOnvifCredentials(SetOnvifCredentialsCommand cmd) {
        ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            String json = """{
    \"onvifUserName\": \"${cmd.onvifUserName}\",
    \"onvifPassword\": \"${cmd.onvifPassword}\"
}
"""
            String fileName = "${grailsApplication.config.camerasHomeDirectory}/onvifCredentials.json"
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(json)

            writer.close()
        }
        catch (Exception ex) {
            logService.cam.error "setCameraAccessCredentials() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }

        return response
    }

    Integer getCameraType(String cameraHost) {
        Integer camType = null
        ObjectCommandResponse getCamerasResult = (ObjectCommandResponse) getCameras()
        if (getCamerasResult.status == PassFail.PASS) {
            JSONObject jo = (JSONObject) getCamerasResult.getResponseObject()
            jo.forEach((k, cam) -> {
                Camera camera = (Camera) cam
                if (Objects.equals(camera.getAddress(), cameraHost))
                    camType = camera.cameraParamSpecs.camType
            })
        }
        return camType
    }

    Camera getCamera(String cameraHost) {
        Camera retVal = null
        ObjectCommandResponse getCamerasResult = (ObjectCommandResponse) getCameras()
        if (getCamerasResult.status == PassFail.PASS) {
            JSONObject jo = (JSONObject) getCamerasResult.getResponseObject()
            jo.forEach((k, cam) -> {
                Camera camera = (Camera) cam
                if (Objects.equals(camera.getAddress(), cameraHost))
                    retVal = camera
            })
        }
        return retVal
    }

    /**
     * getOnvifCredentials: Get the user name and password to authenticate on the cameras Onvif services.
     * These are set up with the setOnvifCredentials call
     *
     * @return The user name
     */
    def getOnvifCredentials() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            FileInputStream fis

            fis = new FileInputStream("${grailsApplication.config.camerasHomeDirectory}/onvifCredentials.json")

            String data = IOUtils.toString(fis, "UTF-8")
            Gson gson2 = new Gson()
            Object obj = gson2.fromJson(data, Object.class)
            response.responseObject = obj
        }
        catch (Exception ex) {
            logService.cam.error "getOnvifCredentials() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }

    def haveOnvifCredentials() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            String pw = null, un = null
            response = getOnvifCredentials()
            if (response.status == PassFail.PASS) {
                un = response.responseObject?.onvifUserName
                pw = response.responseObject?.onvifPassword
            }
            response.responseObject = un != null && pw != null
         }
        catch (Exception ex) {
            String msg = "${ex.getClass().getName()} in haveCameraCredentials: ${ex.getMessage()}"
            response.responseObject = false
            response.status = PassFail.FAIL
            response.error = msg
            logService.getCam().error(msg)
        }
        return response
    }
}
