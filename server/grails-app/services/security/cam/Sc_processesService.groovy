package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.json.JsonSlurper
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import server.Camera

import javax.annotation.PreDestroy
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Transactional
class Sc_processesService {
    LogService logService
    GrailsApplication grailsApplication

    ExecutorService processExecutors
    ArrayList<Process> processes
    boolean running = false

    Sc_processesService() {
        processes = new ArrayList()
        processExecutors = Executors.newCachedThreadPool()
    }

    private List<ProcessHandle> killProcessTree(ProcessHandle ph) {
        List<ProcessHandle> handles = new ArrayList<>()
        ph.children().forEach({ ProcessHandle phc ->
            handles.addAll(killProcessTree(phc))
        })
        ph.destroy()
        handles.add(ph)
        return handles
    }


    def startProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        logService.cam.debug "startProcesses called"
        try {
//            def name = cams['camera1'].name
//            def address = cams['camera1'].address
//            def controlUri = cams['camera1'].controlUri
//            def snapshotUri = cams['camera1'].snapshotUri
//            def streams = cams['camera1'].streams

            running = true
           // startProcess("/usr/bin/motion")
            startProcess("/usr/bin/node /etc/security-cam/nms/app.js")
            Map<String, Camera> cams = getCamerasData()
            String log_dir="/home/security-cam/logs/"      // TODO: Get from config
                    cams.forEach((ck, cam) -> {
                cam.streams.forEach((sk, stream) -> {
                    String[] command = new String[3]
                    command[0] = "bash"
                    command[1] = "-c"
                    command[2] = "/usr/bin/ffmpeg -hide_banner -loglevel error -stimeout 1000000 -rtsp_transport tcp -i ${stream.netcam_uri} -an -c copy -f flv ${stream.nms_uri} 2>> ${log_dir}ffmpeg_${cam.name.replace(' ', '_') + "_" + stream.descr.replace(' ', '_').replace('.', '_')}_\$(date +%Y%m%d).log"
                    startProcess(command)
                })
            })

//            Thread.sleep(60000)
//            stopProcesses()
        }
        catch (Exception ex) {
            logService.cam.error "Exception in startProcesses: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }

    void startProcess(final String command)
    {
        submitTask(command)
    }

    void startProcess(final String[] command)
    {
        submitTask(command)
    }

    private void submitTask(def command)
    {
        processExecutors.submit(() -> {
            do {
                try {
                    Process proc
                    if(command instanceof String )
                        proc = Runtime.getRuntime().exec(command)
                    else if(command instanceof String[])
                        proc = Runtime.getRuntime().exec(command)
                    else
                        break;
                    proc.waitFor(100, TimeUnit.MILLISECONDS)

                    if (proc.isAlive())
                        processes.add(proc)
                    proc.waitFor()
                }
                catch (Exception ex) {
                    logService.cam.error "${ex.getClass().getName()} in startProcess: " + ex.getMessage()
                }
                Thread.sleep(1000)
            }
            while(running)
        })
    }

    @PreDestroy
    void stopProcesses()
    {
        running = false
        processExecutors.shutdownNow()
        processes.forEach(process -> {
            process.descendants().forEach(desc -> desc.destroy())
            process.destroy()
        })
    }

    private Map<String, Camera> getCamerasData()
    {
        File file = new File("${grailsApplication.config.camerasHomeDirectory}/cameras.json")
        JsonSlurper parser = new JsonSlurper()
        def json = parser.parse(file)
        return json as Map<String, Camera>
    }
}
