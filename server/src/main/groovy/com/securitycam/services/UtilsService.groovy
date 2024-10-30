package com.securitycam.services

import com.securitycam.audiobackchannel.RtspClient
import com.securitycam.commands.StartAudioOutCommand
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import jakarta.validation.Valid
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ResourceUtils
import org.springframework.web.bind.annotation.RequestBody

import java.nio.charset.StandardCharsets
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
    public final passwordRegex = /^[A-Za-z0-9][A-Za-z0-9(){\[1*£$\\\]}=@~?^]{7,31}$/
    public final usernameRegex = /^[a-zA-Z0-9](_(?!(.|_))|.(?!(_|.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$/
    public final emailRegex = /^([a-zA-Z0-9_\-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([a-zA-Z0-9\-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$/
    public static final onvifBaseAddressRegex = /^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))($|:([0-9]{1,4}|6[0-5][0-5][0-3][0-5])$)/

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
    ObjectCommandResponse getUserAuthorities() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            if(SecurityContextHolder.getContext().getAuthentication() == null) {
                def ex = new Expando()
                ex.setProperty('authority', 'ROLE_CLIENT')
                result.responseObject = [ex.properties]  // In development/debug mode
            }
            else  // Logged in
                result.responseObject = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
        }
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in getUserAuthorities: ${ex.getCause()} ${ex.getMessage()}")
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    /**
     * getVersion: Get the version from the config file application.yml. This version is generated by
     *             by git describe --tags
     * @return: The version string
     */
    def getVersion() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            File verFile = ResourceUtils.getFile("classpath:version.txt")
            List verLst = verFile.readLines()
            Version ver = new Version(verLst.get(0))
            result.responseObject = ver
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getVersion: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    def getOpenSourceInfo() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            @Language("HTML")
            String response = '''
        <h3>Angular web framework v18.2.1</h3>
        <a href="https://angular.io" target="_blank">Angular</a>
        <hr>
        <h3>Angular Material v18.2.2</h3>
        <a href="https://material.angular.io/" target="_blank">Angular Material</a>
        <hr>
        <h3>Angular Forms v18.2.0</h3>
        <a href="https://angular.io/guide/forms-overview/" target="_blank">Angular Forms</a>
        <hr>
        <h3>Stomp.js v7.0.0</h3>
        <a href="https://www.npmjs.com/package/@stomp/stompjs" target="_blank">Stomp.js</a>
        <hr>
        <h3>File Saver v2.0.5</h3>
        <a href="https://www.npmjs.com/package/@types/file-saver" target="_blank">File Saver</a>
        <hr>
        <h3>hls.js v1.4.12</h3>
        <a href="https://www.npmjs.com/package/hls.js?utm_source=cdnjs&utm_medium=cdnjs_link&utm_campaign=cdnjs_library" target="_blank">Hls.js</a>
        <hr>
        <h3>moment v2.29.1</h3>
        <a href="https://www.npmjs.com/package/moment" target="_blank">moment</a>
        <hr>
        <h3>Object Hash v3.0.0</h3>
        <a href="https://www.npmjs.com/package/object-hash" target="_blank">Object Hash</a>
        <hr>
        <h3>rx v4.1.0</h3>
        <a href="https://www.npmjs.com/package/rx" target="_blank">rx</a>
        <hr>
        <h3>rxjs v6.6.0</h3>
        <a href="https://www.npmjs.com/package/rxjs" target="_blank">rxjs</a>
        <hr>
        <h3>rxjs-observe v2.1.5</h3>
        <a href="https://www.npmjs.com/package/rxjs-observe" target="_blank">rxjs-observ</a>
        <hr>
        <h3>tslib v2.0.0</h3>
        <a href="https://www.npmjs.com/package/tslib" target="_blank">tslib</a>
        <hr>
        <h3>zone.js v0.14.10</h3>
        <a href="https://www.npmjs.com/package/zone.js?activeTab=readme" target="_blank">zone.js</a>
        <hr>
        <h3>typescript v5.4.5</h3>
        <a href="https://www.npmjs.com/package/zone.js?activeTab=readme" target="_blank">zone.js</a>
        <hr>
        <h3>Spring Boot v3.3.4</h3>
        <a href="https://spring.io/projects/spring-boot" target="_blank">Spring Boot</a>
        <hr>
        <h3>Java OpenJDK v21.0.4</h3>
        <a href="https://openjdk.org/" target="_blank">Java OpenJDK</a>
        <hr>
        <h3>ffmpeg v6.1.1</h3>
        <a href="https://www.ffmpeg.org/" target="_blank">ffmpeg</a>
        <hr>
        <h3>Onvif for Java</h3>
        <a href="https://github.com/fpompermaier/onvif" target="_blank">Onvif for Java</a>
        <hr>
        <h3>Motion Service v4.6.0 (for motion detection)</h3>
        <a href="https://motion-project.github.io/" target="_blank">Motion Service</a>
        <hr>
        <h3>nginx v1.24.0 (Reverse proxy)</h3>
        <a href="https://nginx.org/en/" target="_blank">nginx</a>
        <hr>
        <h3>Apache Tomcat vTomcat/9.0.93 (Web Server)</h3>
        <a href="https://tomcat.apache.org/" target="_blank">Apache Tomcat</a>
        <hr>
        <h3>libraspberrypi-bin</h3>
        <a href="https://packages.ubuntu.com/focal-updates/misc/libraspberrypi-bin" target="_blank">libraspberrypi-bin</a>
        <hr>
        <h3>Chrony v4.5 (NTP Server)</h3>
        <a href="https://chrony-project.org/news.html" target="_blank">Chrony</a>
        <hr>
        <h3>Ubuntu network-manager v1.46.4</h3>
        <a href="https://ubuntu.com/core/docs/networkmanager" target="_blank">Network Manager</a>
        <hr>
        <h3>wireless-tools v30 (Wireless Tools for Linux)</h3>
        <a href="https://hewlettpackard.github.io/wireless-tools/Tools.html" target="_blank">Wireless Tools</a>
        <hr>
        <h3>moreutils</h3>
        <a href="https://ostechnix.com/moreutils-collection-useful-unix-utilities/" target="_blank">Moreutils</a>
        <hr>
        <h3>Python3 v 3.12.3</h3>
        <a href="https://www.python.org/" target="_blank">Python3</a>
        <hr>
    '''
            result.response = response

        }
        catch (Exception ex) {
            logService.cam.error("Exception in getOpenSourceInfo: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
