package security.cam

import grails.gorm.transactions.Transactional

import javax.annotation.PreDestroy

@Transactional
class Sc_processesService {

    Sc_processesService()
    {
    }

    Long pid = null

    def startProcesses() {
        if(pid == null) {
            // There should be no processes running at this point, run killall to make sure they are all off
            Process p = Runtime.getRuntime().exec("killall sc_processes.sh")
            p.waitFor()

            p = Runtime.getRuntime().exec("/home/richard/security-cam/xtrn-scripts-and-config/sc_processes.sh")
            pid = p.pid()
        }
    }

    @PreDestroy
    def stopProcesses()
    {
        if(pid != null) {
            Process p = Runtime.getRuntime().exec("kill -INT ${pid}")
            p.waitFor()
            pid = null
        }
    }
}
