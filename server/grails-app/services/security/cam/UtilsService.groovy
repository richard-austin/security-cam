package security.cam

import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class UtilsService {
    LogService logService

    /**
     * executeLinuxCommand: Execute a linux command
     * @param command the command and its parameters as a string
     * @return: The returned value
     */
    String executeLinuxCommand(String command)
    {
        Process p = Runtime.getRuntime().exec(command)
        p.waitFor()

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuffer sb = new StringBuffer()
        String line
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n")
        }

        return sb.toString()
    }

    /**
     *getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    def getTemperature() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            result.responseObject = executeLinuxCommand("vcgencmd measure_temp")
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in getTemperature: "+ex.getCause()+ ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
