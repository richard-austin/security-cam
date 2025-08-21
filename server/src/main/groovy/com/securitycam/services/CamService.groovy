package com.securitycam.services

import com.google.gson.Gson
import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.interfaceobjects.ObjectCommandResponse
import org.apache.cxf.helpers.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.securitycam.enums.PassFail

@Service
class CamService {
    @Autowired
    LogService logService

    @Autowired
    Config config
    @Autowired
    UtilsService utilsService

    /**
     * getCameras: Get all cameras defined in the application.yml file
     * @return
     */
    def getCameras() {
        return utilsService.objectFromFile("${config.camerasHomeDirectory}/cameras.json", "getCameras")
    }

    Integer getCameraType(String cameraHost) {
        Integer camType = 0
        ObjectCommandResponse getCamerasResult = (ObjectCommandResponse) getCameras()
        if (getCamerasResult.status == PassFail.PASS) {

            (getCamerasResult.getResponseObject() as Map<String, Camera>).forEach (k, v) -> {
                if (v.address == cameraHost)
                    camType = v.cameraParamSpecs.camType
            }
        }
        return camType
    }

    Camera getCamera(String cameraHost) {
        Camera retVal = null
        if (!cameraHost.contains("localhost")) {
            ObjectCommandResponse getCamerasResult = (ObjectCommandResponse) getCameras()
            if (getCamerasResult.status == PassFail.PASS) {
                (getCamerasResult.getResponseObject() as Map<String, Camera>).forEach (k, v) -> {
                    if (v.address == cameraHost)
                        retVal = v
                }
                if (retVal == null)
                    logService.cam.error("getCamera: Could not find a camera with the address ${cameraHost}")
            }
        }
        return retVal
    }

    def getPublicKey() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            File file = new File("/etc/security-cam/id_rsa.pub")
            byte[] bytes = file.readBytes()

            // Make into string representation of an array of signed bytes. (This is to keep the format the same
            //  as the previously used Grails render function returns a byte array
            // TODO: Look at returning the raw binary (bytes) and modify the client
            StringBuilder retVal = new StringBuilder()
            retVal.append('[')
            boolean first = true
            for (byte val in bytes) {
                if (first) {
                    first = false
                } else
                    retVal.append(', ')
                retVal.append(String.format("%d", val))
            }
            retVal.append(']')
            result.responseObject = retVal
        }
        catch(Exception ex) {
            logService.cam.error "getPublicKey() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
