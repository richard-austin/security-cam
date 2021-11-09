package security.cam

import grails.gorm.transactions.Transactional
import grails.util.Environment
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import javax.annotation.PreDestroy
import java.util.concurrent.TimeUnit

@Transactional
class Sc_processesService {
    LogService logService

    Sc_processesService() {
    }

    Process p
    Long pid = null

    private List<ProcessHandle> killProcessTree(ProcessHandle ph) {
        List<ProcessHandle> handles = new ArrayList<>()
        ph.children().forEach({ ProcessHandle phc ->
            handles.addAll(killProcessTree(phc))
        })
        ph.destroy()
        handles.add(ph)
        return handles
    }

    private killProcesses(ProcessHandle handle) {
        List<ProcessHandle> handles = killProcessTree(handle)
        List<ProcessHandle> remainingHandles = new ArrayList<ProcessHandle>()
        int testCount = 1200

        while (--testCount > 0) {
            Thread.sleep(20)
            handles.forEach({ ProcessHandle phr ->
                if (phr.isAlive())
                    remainingHandles.add(phr)
            })

            handles.clear()
            handles.addAll(remainingHandles)
            if (remainingHandles.size() > 0) {
                remainingHandles.clear()
            } else
                break
        }

        // If any still remain, destroy forcibly
        handles.forEach({ ProcessHandle rph ->
            rph.destroyForcibly()
            logService.cam.error("Process ${rph.pid()} (${rph.toString()}) remains running, kill signal sent")
        })
    }

    def startProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            if (pid == null) {
                // There should be no processes running at this point, run killall to make sure they are all off
                p = Runtime.getRuntime().exec("killall sc_processes.sh")
                p.waitFor()

                if (Environment.current.name == 'development')
                    p = Runtime.getRuntime().exec("../xtrn-scripts-and-config/sc_processes.sh")
                else if (Environment.current.name == 'production')
                    p = Runtime.getRuntime().exec("/home/security-cam/sc_processes.sh")

                p.waitFor(100, TimeUnit.MILLISECONDS)
                pid = p.pid()
            }
        }
        catch (Exception ex) {
            logService.cam.error "Exception in startProcesses: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
            pid = null
        }
        return response
    }

    @PreDestroy
    def stopProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            logService.cam.debug "stopProcessses: pid = " + pid + ": kill -INT ${pid}"

            killProcesses(p.toHandle())
            pid = null
        }
        catch (Exception ex) {
            logService.cam.error "Exception in stopProcesses: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }

    boolean isRunning() {
        try {
            p.exitValue()
            return false
        } catch (ignored) {
            return true
        }
    }
}
