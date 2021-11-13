package security.cam

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.apache.commons.io.IOUtils
import security.cam.commands.SetAccessCredentialsCommand
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

    /**
     * getCameras: Get all cameras defined in the application.yml file
     * @return
     */
    def getCameras() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            FileInputStream fis

            if (Environment.current.name == 'development')
                fis = new FileInputStream("../xtrn-scripts-and-config/cameras_dev.json")
            else if (Environment.current.name == 'production')
                fis = new FileInputStream("${grailsApplication.config.camerasHomeDirectory}/cameras.json")
            else
                throw new Exception('Unknown environment, expecting production or development')

            String data = IOUtils.toString(fis, "UTF-8")
            Gson gson2 = new Gson()
            Object obj = gson2.fromJson(data, Object.class)

            result.setResponseObject(obj)
        }
        catch (Throwable ex) {
            logService.cam.error "Exception in getCameras -> parse: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = "The config file is corrupt, empty or does not exist. You can create a new config file using the Configure Camera Setup option under the General menu.   ....   " + ex.getMessage()
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
            if (Environment.current.name == 'development')
                fileName = "../xtrn-scripts-and-config/cameras_dev.json"
            else if (Environment.current.name == 'production')
                fileName = "${grailsApplication.config.camerasHomeDirectory}/cameras.json"
            else
                throw new Exception('Unknown environment, expecting production or development')

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(prettyJsonString)

            writer.close()

            sc_processesService.stopProcesses()
            Thread.sleep(500)
            configurationUpdateService.generateConfigs()
            sc_processesService.startProcesses()

            Gson gson2 = new Gson()
            LinkedTreeMap<String, Camera> obj = gson2.fromJson(prettyJsonString, Object.class) as LinkedTreeMap<String, Camera>

            removeUnusedMaskFiles(obj, grailsApplication)

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
            logService.cam.error "CamController.uploadMaskFile() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    /**
     * setCameraAccessCredentials: Set the access credentials used for administrative operations (and snapshot access)
     *                             on the cameras. Note that ths does not change credentials on any camera, just those
     *                             used on this software to access them. Ideally all cameras should use the same user
     *                             name and password.
     * @param cmd: Command object containing the username and password
     * @return: ObjectCommandResponse with success/error state.
     */
    def setCameraAccessCredentials(SetAccessCredentialsCommand cmd) {
        ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            String json = """{
    \"camerasAdminUserName\": \"${cmd.camerasAdminUserName}\",
    \"camerasAdminPassword\": \"${cmd.camerasAdminPassword}\"
}
"""
            String fileName = "/home/security-cam/cameraCredentials.json"
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(json)

            writer.close()
        }
        catch (Exception ex) {
            logService.cam.error "CamController.uploadMaskFile() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }
}
