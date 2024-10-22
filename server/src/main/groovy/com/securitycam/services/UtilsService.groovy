package com.securitycam.services

import com.securitycam.audiobackchannel.RtspClient
import com.securitycam.commands.StartAudioOutCommand
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody

import java.util.concurrent.ConcurrentLinkedQueue

class Temperature {
    Temperature(String temp) {
        this.temp = temp
    }

    String temp
}

class Version {
    Version(String version) {
        this.version = version
    }

    String version
}

class MyIP {
    MyIP(String myIp) {
        this.myIp = myIp
    }

    String myIp
}

@Service
@Transactional
@EnableScheduling
class UtilsService {
    @Autowired
    LogService logService

    @Autowired
    Environment environment

    @Autowired
    SimpMessagingTemplate brokerMessagingTemplate

    Queue<byte[]> audioQueue = new ConcurrentLinkedQueue<>()
    /**
     * executeLinuxCommand: Execute a linux command
     * @param command the command and its parameters as a string
     * @return: The returned value
     */
    static String executeLinuxCommand(String command) {
        Process p = new ProcessBuilder(command).start() // Process p = Runtime.getRuntime().exec(command)
        String retVal = processCommandOutput(p)
        p.waitFor()
        retVal
    }

    /**
     * executeLinuxCommand: Execute a linux command
     * @param command the command and its parameters as a string array (used for specifying a shell to execute the command)
     * @return: The returned value
     */
    static String executeLinuxCommand(String[] command) {
        Process p = new ProcessBuilder(command).start() // Runtime.getRuntime().exec(command)
        String retVal = processCommandOutput(p)
        p.waitFor()
        retVal
    }

    private static String processCommandOutput(Process p) {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()))
        StringBuffer sb = new StringBuffer()
        String line
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n")
        }

        return sb.toString()
    }


    /**
     * getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    def getTemperature() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            if (environment.getProperty("spring.profiles.active") == "dev") {
                // There is no vcgencmd on a PC, only raspberry pi
                Temperature temp = new Temperature("temp=45.2'C")
                result.responseObject = temp
            } else {
                Temperature temp = new Temperature(executeLinuxCommand("vcgencmd", "measure_temp"))
                result.responseObject = temp
            }
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getTemperature: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    boolean audioInUse = false
    RtspClient client

    private ServerSocket serverSocket
    private Socket clientSocket
    private OutputStream out
    Thread serverThread

    def startAudioOut(@Valid @RequestBody StartAudioOutCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        if (!audioInUse) {
            try {
                // Set up the socket for ffmpeg backchannel feed to source from
                serverSocket = new ServerSocket(8881)
                serverThread = new Thread(() -> {
                    try {
                        clientSocket = serverSocket.accept()
                        out = clientSocket.getOutputStream()
                    }
                    catch(Exception ex) {
                        stopAudioOut()
                        logService.cam.error "${ex.getClass().getName()} in startAudioOut accept thread: ${ex.getMessage()}"
                    }
                })
                serverThread.start()

                final String talkOff = new JSONObject()
                        .put("message", "talkOff")
                        .put("instruction", "on")
                        .toString()
                // Disable audio out on clients except the initiator
                brokerMessagingTemplate.convertAndSend("/topic/talkoff", talkOff)

                final URI netcam_uri = new URI(cmd.netcam_uri)
                CameraAdminCredentials creds  = cmd.cam.credentials()
                client = new RtspClient(netcam_uri.getHost(), netcam_uri.getPort(), creds.userName, creds.password, logService)
                client.start()
                result = client.await()
            }
            catch (Exception ex) {
                stopAudioOut()
                logService.cam.error "${ex.getClass().getName()} in startAudioOut: ${ex.getMessage()}"
                result.status = PassFail.FAIL
                result.error = "${ex.getClass().getName()} -- ${ex.getMessage()}"
            }
            audioInUse = true
        }
        return result
    }

    def stopAudioOut() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            final String talkOff = new JSONObject()
                    .put("message", "talkOff")
                    .put("instruction", "off")
                    .toString()
            // Re-enable audio out on clients
            brokerMessagingTemplate.convertAndSend("/topic/talkoff", talkOff)
            audioInUse = false
            audioQueue.clear()

            serverSocket?.close()
            clientSocket?.close()
            serverThread?.interrupt()
            out?.close()
            out = null
            client?.stop()
            client = null
        }
        catch (Exception ex) {
            logService.cam.error "${ex.getClass().getName()} in stopAudioOut: ${ex.getMessage()}"
            result.status = PassFail.FAIL
            result.error = "${ex.getClass().getName()} -- ${ex.getMessage()}"
        }
        return result
    }

    private final long audioInSessionTimeout = 3
    // Stop audio input session after 3 seconds without a websocket message
    private long audioInputStreamCheckCount = 0
    def audio(byte[] bytes) {
        audioQueue.add(bytes)
        // Reset the stream check count to indicate the stream is still active
        audioInputStreamCheckCount = 0
        if (out) {
            try {
                while (!audioQueue.empty) {
                    out.write(audioQueue.poll())
                    Thread.sleep(3)
                    //   System.out.println("Writing ${audioQueue.poll().length} bytes")
                }
            }
            catch (Exception ex) {
                System.out.println("${ex.getClass().getName()} in audio handler: ${ex.getMessage()}")
            }
        }
    }

}
