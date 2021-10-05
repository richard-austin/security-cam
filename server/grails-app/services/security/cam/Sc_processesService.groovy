package security.cam

import grails.gorm.transactions.Transactional
import grails.util.Environment
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import javax.annotation.PreDestroy

@Transactional
class Sc_processesService {
    LogService logService

    Sc_processesService()
    {
    }

    Long pid = null

    def startProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            if (pid == null) {
                // There should be no processes running at this point, run killall to make sure they are all off
                Process p = Runtime.getRuntime().exec("killall sc_processes.sh")
                p.waitFor()

                if (Environment.current.name == 'development')
                    p = Runtime.getRuntime().exec("../xtrn-scripts-and-config/sc_processes.sh")
                else if (Environment.current.name == 'production')
                    p = Runtime.getRuntime().exec("/etc/security-cam/sc_processes.sh")

                pid = p.pid()
            }
        }
        catch(Exception ex)
        {
            logService.cam.error "Exception in startProcesses: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }

    @PreDestroy
    def stopProcesses()
    {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            if (pid != null) {
                Process p = Runtime.getRuntime().exec("kill -INT ${pid}")
                p.waitFor()
                pid = null
            }
        }
        catch(Exception ex)
        {
            logService.cam.error "Exception in stopProcesses: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }
}
