package security.cam

import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class UtilsService {
    LogService logService

    /**
     *getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    def getTemperature() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Process p = Runtime.getRuntime().exec("vcgencmd measure_temp")
            p.waitFor()

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuffer sb = new StringBuffer()
            String line
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n")
            }

            result.responseObject = sb.toString()
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
