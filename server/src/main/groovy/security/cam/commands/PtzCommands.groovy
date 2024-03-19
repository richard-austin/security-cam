package security.cam.commands

import com.fasterxml.jackson.databind.ObjectMapper
import grails.validation.Validateable
import security.cam.LogService
import security.cam.interfaceobjects.Asymmetric
import server.CameraAdminCredentials

class PtzCommands implements Validateable {
    String creds
    String user, password
    LogService logService
    // These are not Restful; API parameters, only creds (encrypted) form is sent from the client.

    static constraints = {
        creds(nullable: true, blank: true,
                validator: { creds, cmd ->
                    {
                        if (creds != null && creds != "") {
                            // Decrypt creds and set the user name and password
                            Asymmetric asym = new Asymmetric()
                            String jsonCreds = asym.decrypt(creds)
                            ObjectMapper mapper = new ObjectMapper()
                            cmd.logService.cam.info("Before decryption")
                            if (jsonCreds.length() > 0) {
                                CameraAdminCredentials cac = mapper.readValue(jsonCreds, CameraAdminCredentials.class)
                                cmd.user = cac.userName
                                cmd.password = cac.password
                                cmd.logService.cam.info("After decryption")
                            }
                        }
                        return  // No validation errors
                    }
                })
    }
}
