package security.cam

import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class UtilsService {
    LogService logService

    /**
     *
     * @return
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
