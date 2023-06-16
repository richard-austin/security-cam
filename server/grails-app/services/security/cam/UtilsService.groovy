package security.cam

import asset.pipeline.grails.AssetResourceLocator
import com.google.gson.GsonBuilder
import grails.config.Config
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.grails.web.json.JSONObject
import org.springframework.core.io.Resource
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import security.cam.audiobackchannel.RtspClient
import security.cam.commands.SMTPData
import security.cam.commands.SetupSMTPAccountCommand
import security.cam.commands.StartAudioOutCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.UserPrincipalLookupService

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

@Transactional
@EnableScheduling
class UtilsService {
    LogService logService
    AssetResourceLocator assetResourceLocator
    GrailsApplication grailsApplication
    SimpMessagingTemplate brokerMessagingTemplate

    public final passwordRegex = /^[A-Za-z0-9][A-Za-z0-9(){\[1*£$\\\]}=@~?^]{7,31}$/
    public final usernameRegex = /^[a-zA-Z0-9](_(?!(.|_))|.(?!(_|.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$/
    public final emailRegex = /^([a-zA-Z0-9_\-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([a-zA-Z0-9\-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$/
    public static final onvifBaseAddressRegex = /^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))($|:([0-9]{1,4}|6[0-5][0-5][0-3][0-5])$)/

    /**
     * executeLinuxCommand: Execute a linux command
     * @param command the command and its parameters as a string
     * @return: The returned value
     */
    String executeLinuxCommand(String command) {
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
    String executeLinuxCommand(String[] command) {
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
            if(Environment.isDevelopmentMode()) {
                // There is no vcgencmd on a PC, only raspberry pi
                Temperature temp = new Temperature("temp=45.2'C")
                result.responseObject = temp
            }
            else {
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

    /**
     * getVersion: Get the version from the config file application.yml. This version is generated by
     *             by git describe --tags
     * @return: The version string
     */
    def getVersion() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Resource verRes = assetResourceLocator.findResourceForURI('./version.txt')
            String verStr = new String(verRes?.getInputStream()?.bytes, StandardCharsets.UTF_8)
            Version ver = new Version(verStr)
            result.responseObject = ver
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getVersion: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    /**
     * setIP: Set the file myip to contain our current public ip address.
     * @return: Our public ip address
     */
    ObjectCommandResponse setIP() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            InputStream is = new URL("https://api.ipify.org").openStream()
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A")

            String baseDir = grailsApplication.config.camerasHomeDirectory
            Path myipFile = Paths.get(baseDir as String, 'myip')

            String myIp = s.next()
            //Write the ip address to the file
            BufferedWriter writer = new BufferedWriter(new FileWriter(myipFile.toString()))
            writer.write(myIp)
            writer.close()
            s.close()
            is.close()
            result.responseObject = new MyIP(myIp)

            // Make the myip file a member of the security-cam group
            String secCam = "security-cam"
            UserPrincipalLookupService lookupService = FileSystems.getDefault()
                    .getUserPrincipalLookupService()
            GroupPrincipal group = lookupService.lookupPrincipalByGroupName(secCam)
            Files.getFileAttributeView(myipFile, PosixFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS).setGroup(group)
        }
        catch (IOException e) {
            logService.cam.error "${e.getClass().getName()} in setIP: ${e.getMessage()}"
            result.status = PassFail.FAIL
            result.error = "${e.getClass().getName()} -- ${e.getMessage()}"
        }
        return result
    }

    ObjectCommandResponse setupSMTPClient(SetupSMTPAccountCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create()

            String res = gson.toJson(cmd.getData())
            def writer = new BufferedWriter(new FileWriter("/var/security-cam/smtp.json"))
            writer.write(res)
            writer.close()
        }
        catch (Exception e) {
            logService.cam.error "${e.getClass().getName()} in setupSMTPClient: ${e.getMessage()}"
            result.status = PassFail.FAIL
            result.error = "${e.getClass().getName()} -- ${e.getMessage()}"
        }
        return result
    }

    ObjectCommandResponse getSMTPClientParams() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            result.responseObject = getSMTPConfigData()
        }
        catch (Exception e) {
            logService.cam.error "${e.getClass().getName()} in getSMTPClientParams: ${e.getMessage()}"
            result.status = PassFail.FAIL
            result.error = "${e.getClass().getName()} -- ${e.getMessage()}"
        }
        return result
    }

    def getSMTPConfigData() {
        Config config = grailsApplication.getConfig()
        def configFileName = config.getProperty("mail.smtp.configFile")
        File file = new File(configFileName)
        byte[] bytes = file.readBytes()
        String json = new String(bytes, StandardCharsets.UTF_8)
        def gson = new GsonBuilder().create()
        def smtpData = gson.fromJson(json, SMTPData)
        return smtpData
    }


    // Execute every second. If the audio input feed is running it increments the audioInputStreamCheckCount which is
    //  zeroed by each time an audio packet is received. If the count gets past 3, the audio feed is assumed to have
    //  stopped (maybe by the audio session not being closed cleanly), so it stops the feed properly.
    @Scheduled(fixedRate = 1000L)
    def audioFeedCheck() {
        if (audioInUse) {
            // Shut down the sudio receiver if the websocket data stream has stopped
            if (++audioInputStreamCheckCount > audioInSessionTimeout) {
                stopAudioOut()
            }
        }
    }

    Process audioOutProc
    File fifo
    FileOutputStream fos
    boolean audioInUse = false
    RtspClient client

    synchronized def startAudioOut(StartAudioOutCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        if (!audioInUse) {
            try {
                final String talkOff = new JSONObject()
                        .put("message", "talkOff")
                        .put("instruction", "on")
                        .toString()
                // Disable audio out on clients except the initiator
                brokerMessagingTemplate.convertAndSend("/topic/talkoff", talkOff)

                stopAudioOut(false)
                String fifoPath = grailsApplication.config.twoWayAudio.fifo
                if (fifoPath == null)
                    throw new Exception("No path is specified for twoWayAudio: fifo in the configuration")
                // Create the fifo
                fifo = createFifoPipe(fifoPath)
                client = new RtspClient("192.168.1.43", 554, "admin", "R@nc1dTapsB0ttom", logService)
                client.sendReq(grailsApplication)

                // Create a file output stream for the websocket handler to write to the fifo
                fos = new FileOutputStream(fifo)
            }
            catch (Exception ex) {
                logService.cam.error "${ex.getClass().getName()} in startAudioOut: ${ex.getMessage()}"
                result.status = PassFail.FAIL
                result.error = "${ex.getClass().getName()} -- ${ex.getMessage()}"
            }
            audioInUse = true
        }
        return result
    }

    def stopAudioOut(boolean sendTalkOff = true) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            if (fos)
                fos.close()
            // Remove the fifo
            if (fifo)
                fifo.delete()

            if (sendTalkOff) {
                final String talkOff = new JSONObject()
                        .put("message", "talkOff")
                        .put("instruction", "off")
                        .toString()
                // Re-enable audio out on clients
                brokerMessagingTemplate.convertAndSend("/topic/talkoff", talkOff)
                audioInUse = false
                client.stop()
                client = null
            }
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
        if (fos) {
            // Reset the stream check count to indicate the stream is still active
            audioInputStreamCheckCount = 0
            try {
                fos.write(bytes)
            }
            catch(Exception ignore) {}
        }
    }

    // This timer ends the audio input session if no websocket messages are received for more than 3 seconds
    private static File createFifoPipe(String fifoName) throws IOException, InterruptedException {
        String[] command = new String[]{"mkfifo", fifoName}
        def process = new ProcessBuilder(command).start()
        process.waitFor()
        return new File(fifoName)
    }
}
