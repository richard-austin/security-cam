package com.securitycam.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import com.securitycam.audiobackchannel.RtspClient
import com.securitycam.commands.SMTPData
import com.securitycam.commands.SetupSMTPAccountCommand
import com.securitycam.commands.StartAudioOutCommand
import com.securitycam.commands.UpdateAdHocDeviceListCommand
import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.validation.Valid
import org.apache.cxf.helpers.IOUtils
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.core.env.Environment
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestBody

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.UserPrincipalLookupService
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
class Device  {
    String name
    String ipAddress
    int ipPort
}

@Service
@EnableScheduling
class UtilsService {
    @Autowired
    LogService logService

    @Autowired
    Environment environment

    @Autowired
    SimpMessagingTemplate brokerMessagingTemplate

    @Autowired
    Config config

    private byte[] firstPacket
    private int iteration = 0
    public static final passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@\u0024!%*#?&])[A-Za-z\d@\u0024!%*#?&]{8,}\u0024/
    public static final usernameRegex = /^[a-zA-Z0-9](_(?!(.|_))|.(?!(_|.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$/
    public static final emailRegex = /^([a-zA-Z0-9_\-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([a-zA-Z0-9\-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$/
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
            result.responseObject.confirmPassword = result.responseObject.password = ""
        }
        catch (FileNotFoundException fnf) {
            final String noConfigFile = "No existing config file for SMTP client. It will be created when the SMTP parameters are entered and saved."
            logService.cam.warn(noConfigFile)
            result.status = PassFail.PASS
            result.response = noConfigFile
            result.error = "${fnf.getClass().getName()} -- ${fnf.getMessage()}"
        }
        catch (Exception e) {
            logService.cam.error "${e.getClass().getName()} in getSMTPClientParams: ${e.getMessage()}"
            result.status = PassFail.FAIL
            result.response = null
            result.error = "${e.getClass().getName()} -- ${e.getMessage()}"
        }
        return result
    }

    def getSMTPConfigData() {
        def configFileName = config.mail.smtp.configFile
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
                    catch (Exception ex) {
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
                CameraAdminCredentials creds = cmd.cam.credentials()
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
            iteration = 0

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
        if(++iteration== 1) {
            firstPacket = new byte[bytes.size()]
            System.arraycopy(bytes, 0, firstPacket, 0, bytes.size())
        }

        // Reset the stream check count to indicate the stream is still active
        audioInputStreamCheckCount = 0
        if (out) {
            try {
                if(firstPacket) {
                    out.write(firstPacket)
                    firstPacket = null
                }
                if(iteration > 3)
                    out.write(bytes)
            }
            catch (Exception ex) {
                System.out.println("${ex.getClass().getName()} in audio handler: ${ex.getMessage()}")
            }
        }
    }

    ObjectCommandResponse getUserAuthorities() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                def ex = new Expando()
                ex.setProperty('authority', 'ROLE_CLIENT')
                result.responseObject = [ex.properties]  // In development/debug mode
            } else  // Logged in
                result.responseObject = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
        }
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in getUserAuthorities: ${ex.getCause()} ${ex.getMessage()}")
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    def objectFromFile(String path, String methodName) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            FileInputStream fis
            fis = new FileInputStream(path)
            String data = IOUtils.toString(fis, "UTF-8")
            Gson gson2 = new Gson()
            Object obj = gson2.fromJson(data, Object.class)
            result.setResponseObject(obj)
        }
        catch(Exception ex) {
            logService.cam.error "Exception in ${methodName}: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result

    }
    def loadAdHocDevices() {
        return objectFromFile("${config.camerasHomeDirectory}/adhocdevices.json", "loadAdHocDevices")
    }

    /**
     * updateAdHocDeviceList: Update the list of devices for whose web admin page will be viewable on the NVR. This hosting
     *                        is similar to that provided automatically for configured cameras, but it can be any device
     *                        with web admin on the same intranet as the NVR.     *
     * @param cmd: Contains the JSON representing the ad hoc devicxxe list
     * @return
     */
    def updateAdHocDeviceList(UpdateAdHocDeviceListCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create()
            JsonElement je = JsonParser.parseString(cmd.adHocDeviceListJSON)
            String prettyJsonString = gson.toJson(je)

            String fileName
            fileName = "${config.camerasHomeDirectory}/adhocdevices.json"

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(prettyJsonString)

            writer.close()

             if (result.status !== PassFail.PASS)
                throw new Exception(result.error)

            Gson gson2 = new Gson()
            ArrayList<Device> obj = gson2.fromJson(prettyJsonString, Object.class) as ArrayList<Device>

            result.setResponseObject(obj)
        }
        catch (Exception ex) {
            logService.cam.error "Exception in updateAdHocDeviceList: " + ex.getMessage()
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
            File verFile = new File("${config.appHomeDirectory}/version.txt")
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
        <h3>Angular web framework v21.0.0</h3>
        <a href="https://angular.io" target="_blank">Angular</a>
        <hr>
        <h3>Angular Material v21.0.0</h3>
        <a href="https://material.angular.io/" target="_blank">Angular Material</a>
        <hr>
        <h3>Angular Forms v21.0.0</h3>
        <a href="https://angular.io/guide/forms-overview/" target="_blank">Angular Forms</a>
        <hr>
        <h3>Stomp.js v7.0.0</h3>
        <a href="https://www.npmjs.com/package/@stomp/stompjs" target="_blank">Stomp.js</a>
        <hr>
        <h3>File Saver v2.0.5</h3>
        <a href="https://www.npmjs.com/package/@types/file-saver" target="_blank">File Saver</a>
        <hr>
        <h3>hls.js v1.5.2</h3>
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
        <h3>zone.js v0.15.0</h3>
        <a href="https://www.npmjs.com/package/zone.js?activeTab=readme" target="_blank">zone.js</a>
        <hr>
        <h3>typescript v5.9.3</h3>
        <a href="https://www.npmjs.com/package/zone.js?activeTab=readme" target="_blank">zone.js</a>
        <hr>
        <h3>Spring Boot v3.5.7</h3>
        <a href="https://spring.io/projects/spring-boot" target="_blank">Spring Boot</a>
        <hr>
        <h3>Java OpenJDK v21.0.8</h3>
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
        <h3>Apache Tomcat/10.1.16 (Ubuntu) (Web Server)</h3>
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

    /**
     * setIP: Set the file myip to contain our current public ip address.
     * @return: Our public ip address
     */
    ObjectCommandResponse setIP() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            InputStream is = new URI("https://api.ipify.org").toURL().openStream()
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A")

            String baseDir = config.camerasHomeDirectory
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

    def sendEmail(String msg, String subject, String recipientAddrs) {
        def smtpData = getSMTPConfigData()

        Properties prop = new Properties()
        prop.put("mail.smtp.auth", smtpData.auth)
        prop.put("mail.smtp.starttls.enable", smtpData.enableStartTLS)
        if (smtpData.enableStartTLS) {
            prop.put("mail.smtp.ssl.protocols", smtpData.sslProtocols)
            prop.put("mail.smtp.ssl.trust", smtpData.sslTrust)
        }
        prop.put("mail.smtp.host", smtpData.host)
        prop.put("mail.smtp.port", smtpData.port)
        prop.put("mail.smtp.connectiontimeout", "10000")
        prop.put("mail.smtp.timeout", "10000")

        logService.cam.trace("mail.smtp.auth=${smtpData.auth}")
        logService.cam.trace("mail.smtp.starttls.enable=${smtpData.enableStartTLS}")
        logService.cam.trace("mail.smtp.ssl.protocols=${smtpData.sslProtocols}")
        logService.cam.trace("mail.smtp.host=${smtpData.host}")
        logService.cam.trace("mail.smtp.port=${smtpData.port}")
        logService.cam.trace("mail.smtp.ssl.trust=${smtpData.sslTrust}")


        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpData.username, smtpData.password)
            }
        })

        FileOutputStream fs = new FileOutputStream("/var/log/security-cam/javaxMailLog.log")
        PrintStream ps = new PrintStream(fs, true)
        session.setDebugOut(ps)
        session.debug = true
        Message message = new MimeMessage(session)
        message.setFrom(new InternetAddress(smtpData.fromAddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientAddrs))
        message.setSubject(subject)

        MimeBodyPart mimeBodyPart = new MimeBodyPart()
        mimeBodyPart.setContent(msg, "text/html; charset=utf-8")

        Multipart multipart = new MimeMultipart()
        multipart.addBodyPart(mimeBodyPart)

        message.setContent(multipart)
        Transport.send(message)
    }
}


