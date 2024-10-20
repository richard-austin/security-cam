package com.securitycam.services

import com.google.gson.Gson
import com.securitycam.configuration.Config
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
    /**
     * getCameras: Get all cameras defined in the application.yml file
     * @return
     */
    def getCameras() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            FileInputStream fis

            fis = new FileInputStream("${config.camerasHomeDirectory}/cameras.json")
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

    def getPublicKey() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            File file = new File("/etc/security-cam/id_rsa.pub")

            byte[] bytes = file.readBytes()
            result.responseObject = bytes
        }
        catch(Exception ex) {
            logService.cam.error "getPublicKey() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
