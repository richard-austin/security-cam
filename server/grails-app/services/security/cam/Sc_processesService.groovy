package security.cam

import grails.config.Config
import grails.plugin.springsecurity.SpringSecurityService
import grails.util.Environment
import security.cam.eventlisteners.IpCheckTimerTask
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse
import server.Camera

import javax.annotation.PreDestroy
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Transactional
class Sc_processesService {
    LogService logService
    RestfulInterfaceService restfulInterfaceService
    GrailsApplication grailsApplication
    SpringSecurityService springSecurityService

    ExecutorService processExecutors
    ArrayList<Process> processes
    OnvifService onvifService

    boolean running = false

    final int ipCheckTimeout = 1000 * 60 * 15  // 15 minutes time between IP checks
    final int emailTimeout = 1000 * 60 * 60  // 1 Hr between sending repeat emails
    Timer ipChangeCheckTimer

    Sc_processesService() {
        processes = new ArrayList()
    }

    /**
     * readMyIp: Read the current public facing IP for the broadband connection
     * @return
     */
    private String readMyIp() {
        String retVal = ""
        try (Scanner s = new Scanner(new URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
            retVal = s.next()
        } catch (IOException ex) {
            logService.cam.error "Exception in readMyIp: " + ex.getMessage()
        }
        return retVal
    }

    /**
     * getSavedIP: Get the IP address stored in the myip file.
     * @return
     */
    private String getSavedIP() {
        String retVal = ""
        File file = new File(grailsApplication.config.myipFileLocation as String)

        try (Scanner reader = new Scanner(file)) {
            if (reader.hasNextLine())
                retVal = reader.nextLine()
        }
        catch (Exception ex) {
            logService.cam.error "${ex.getClass().getName()} in getSavedIP: " + ex.getMessage()
        }

        return retVal
    }

    /**
     * ipCheck: Task called periodically by the IP check timer.
     */
    private void ipCheck() {
        String savedIp = getSavedIP()
        String currentIp = readMyIp()

        if (savedIp != currentIp) {
            ipChangeCheckTimer.cancel()
            logService.cam.warn("Current IP (${currentIp}) does not match the saved IP address (${savedIp})")
            setupEmailTimer()
        }
    }

    /**
     * emailTask: Task called periodically by the email timer
     */
    @Transactional
    private void emailTask() {
        try {
            String savedIp = getSavedIP()
            String currentIp = readMyIp()
            if (savedIp == currentIp) {
                ipChangeCheckTimer.cancel()
                setupIpCheckTimer()   // Set back to the periodic IP check when the saved and current IP are equal
                logService.cam.warn("Current IP (${currentIp}) now matches the saved IP address. Stop sending warning emails")
            } else {
                // IP address does not match the saved IP, send a warning email
                if (Environment.current.name == 'development') {
                    sendEmail("Richard", "richard.david.austin@gmail.com", currentIp)
                } else {
                    User user = User.findByCloudAccount(false)
                    if (user == null)
                        logService.cam.debug("emailTask: User is null")
                    else
                        logService.cam.debug("emailTask: username = ${user.username} email = ${user.email}")

                    if (user != null)
                        sendEmail(user.username, user.email, currentIp)
                }
            }
        }
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in emailTask: ${ex.getMessage()}: Caused by: ${ex.getCause()}")
        }
    }

    /**
     * sendEmail: Send the warning email with the new IP address if the public facing IP address for the broadband connection changes.
     * @param userName : User name used for salutation
     * @param recipientAddress : The address to send te message to.
     * @param currentIP : The current (new) IP address
     * @return
     */
    private void sendEmail(String userName, String recipientAddress, String currentIP) {
        Config config = grailsApplication.getConfig()
        def auth = config.getProperty("mail.smtp.auth")
        def enable = config.getProperty("mail.smtp.starttls.enable")
        def protocols = config.getProperty("mail.smtp.ssl.protocols")
        def host = config.getProperty("mail.smtp.host")
        def port = config.getProperty("mail.smtp.port")
        def trust = config.getProperty("mail.smtp.ssl.trust")
        def smtpUsername = config.getProperty("mail.smtp.username")
        def password = config.getProperty("mail.smtp.password")
        def fromaddress = config.getProperty("mail.smtp.fromaddress")

        Properties prop = new Properties()
        prop.put("mail.smtp.auth", auth)
        prop.put("mail.smtp.starttls.enable", enable)
        prop.put("mail.smtp.ssl.protocols", protocols)
        prop.put("mail.smtp.host", host)
        prop.put("mail.smtp.port", port)
        prop.put("mail.smtp.ssl.trust", trust)

        logService.cam.debug("mail.smtp.auth=${auth}")
        logService.cam.debug("mail.smtp.starttls.enable=${enable}")
        logService.cam.debug("mail.smtp.ssl.protocols=${protocols}")
        logService.cam.debug("mail.smtp.host=${host}")
        logService.cam.debug("mail.smtp.port=${port}")
        logService.cam.debug("mail.smtp.ssl.trust=${trust}")

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                logService.cam.trace("Authenticating: ${smtpUsername}:xxxxxxxxxx")
                return new PasswordAuthentication(smtpUsername, password)
            }
        })
        session.setDebug(true)
        FileOutputStream fs = new FileOutputStream("/var/log/security-cam/javaxMailLog.log")
        PrintStream ps = new PrintStream(fs, true)
        session.setDebugOut(ps)
        Message message = new MimeMessage(session)
        logService.cam.trace("fromaddress: ${fromaddress}")
        message.setFrom(new InternetAddress(fromaddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientAddress))
        message.setSubject("Change of public IP address")

        String msg = """Hi ${userName}.
<div>
    I have detected a change of Virgin Media broadband IP address, this is now ${currentIP}
</div>
<div>
    Please go to the web application at <a href="https://${currentIP}">https://${currentIP}</a> and use the "Save Current Public IP" option  on the General menu to stop these emails continuing to be sent.
</div>
<br>
<div>
    Thanks
</div>
<br>
<div>
    Raspberry pi
</div>
"""
        MimeBodyPart mimeBodyPart = new MimeBodyPart()
        mimeBodyPart.setContent(msg, "text/html; charset=utf-8")

        Multipart multipart = new MimeMultipart()
        multipart.addBodyPart(mimeBodyPart)

        message.setContent(multipart)
        logService.cam.debug("Sending email to ${recipientAddress}")
        Transport.send(message)
    }

    /**
     * setupIpCheckTimer: Set up the timer which checks for changed public IP periodically
     */
    void setupIpCheckTimer() {
        IpCheckTimerTask ictt = new IpCheckTimerTask(() -> ipCheck())
        ipChangeCheckTimer = new Timer("ipCheckTimer")
        ipChangeCheckTimer.scheduleAtFixedRate(ictt, 0, ipCheckTimeout)
    }

    /**
     * setupEmailTimer: Set up the timer which sends warning emails periodically when the public IP address has changed.
     */
    void setupEmailTimer() {
        IpCheckTimerTask ictt = new IpCheckTimerTask(() -> emailTask())
        ipChangeCheckTimer = new Timer("emailTimer")
        ipChangeCheckTimer.scheduleAtFixedRate(ictt, 0, emailTimeout)
    }

    /**
     * startProcesses: Start ode Media Server (nms), the ffmpeg live stream links to nms server, motion sensing process
     *                 and the IP change detection process
     * @return
     */
    def startProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        logService.cam.debug "startProcesses called"
        try {
            setupIpCheckTimer()
            running = true
            processExecutors = Executors.newCachedThreadPool()
            startProcess("/usr/bin/node /etc/security-cam/nms/app.js")
            Map<String, Camera> cams = getCamerasData()
            String log_dir = "/var/log/security-cam/"      // TODO: Get from config
            cams.forEach((ck, cam) -> {
                cam.streams.forEach((sk, stream) -> {
                    String[] command = new String[3]
                    command[0] = "bash"
                    command[1] = "-c"
                    command[2] = "/usr/bin/ffmpeg -hide_banner -loglevel error -stimeout 1000000 -rtsp_transport tcp -i ${stream.netcam_uri} ${stream.audio_bitrate == "0" || stream.audio_bitrate == null ? "-an" : "-c:a aac -ar ${stream.audio_bitrate}"} -c:v copy  -preset superfast -tune zerolatency -f flv ${stream.nms_uri} 2>> ${log_dir}ffmpeg_${cam.name.replace(' ', '_') + "_" + stream.descr.replace(' ', '_').replace('.', '_')}_\$(date +%Y%m%d).log"
                    startProcess(command)
                })
            })

            // Start motion and reload config to take on any changes
            RestfulResponse restResponse = restfulInterfaceService.sendRequest("localhost:8000", "/",
                    "{\"command\": \"start_motion\"}",
                    true, 12000)

            // Populate the onvif device map so the devices don't have to be created each time a PTZ operation is done
            onvifService.populateDeviceMap()

            if(restResponse.responseCode != 200) {
                logService.cam.error("Error starting motion service: ${restResponse.errorMsg}")
                response.status = PassFail.FAIL
                response.error = restResponse.errorMsg
            }
       //     Runtime.getRuntime().exec("pkill --signal SIGHUP motion")

        }
        catch (Exception ex) {
            logService.cam.error "Exception in startProcesses: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }

    /**
     * startProcess: Start a single process
     * @param command
     */
    void startProcess(final String command) {
        submitTask(command)
    }

    /**
     * startProcess: Start a single process with a multiline command (used for starting a process under a bash shell etc)
     * @param command
     */
    void startProcess(final String[] command) {
        submitTask(command)
    }

    /**
     * submitTask: Start a process with the given command line within a restart loop in the thread pool thread.
     * @param command
     */
    private void submitTask(def command) {
        processExecutors.submit(() -> {
            do {
                try {
                    Process proc
                    if (command instanceof String)
                        proc = Runtime.getRuntime().exec(command)
                    else if (command instanceof String[])
                        proc = Runtime.getRuntime().exec(command)
                    else
                        break
                    proc.waitFor(100, TimeUnit.MILLISECONDS)

                    if (proc.isAlive())
                        processes.add(proc)
                    proc.waitFor()
                }
                catch(InterruptedException iex)
                {
                    logService.cam.warn "${iex.getClass().getName()} in submitTask: " + iex.getMessage()
                }
                catch (Exception ex) {
                    logService.cam.error "${ex.getClass().getName()} in submitTask: " + ex.getMessage()
                }
                if (command instanceof String)
                    logService.cam.debug("Process terminated: (${command}}")
                else if (command instanceof String[]) {
                    logService.cam.warn("Process terminated: -")
                    for (String cmd : command)
                        logService.cam.debug("${cmd}")
                }
                if (running)
                    Thread.sleep(1000)
            }
            while (running)
        })
    }

    /**
     * stopProcesses: Stop all started processes and their descendents
     */
    @PreDestroy
    def stopProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            running = false
            RestfulResponse restResponse = restfulInterfaceService.sendRequest("localhost:8000", "/",
                    "{\"command\": \"stop_motion\"}",
                    true, 15000)
            if (restResponse.responseCode != 200) {
                logService.cam.error("Error stopping motion service: ${restResponse.errorMsg}")
                response.status = PassFail.FAIL
                response.error = restResponse.errorMsg
            }

            ipChangeCheckTimer.cancel()
            ipChangeCheckTimer.purge()
            processExecutors.shutdownNow()
            processes.forEach(process -> {
                process.descendants().forEach(desc -> desc.destroy())
                process.destroy()
                while (process.isAlive()) {
                    Thread.sleep(20)
                }
            })
        }
        catch(Exception ex)
        {
            logService.cam.error("${ex.getClass().getName()} in stopProcesses: ${ex.getMessage()}")
            response.status = PassFail.FAIL
            response.error = "${ex.getClass().getName()} in stopProcesses: ${ex.getMessage()}"
        }
        return response
    }

    private Map<String, Camera> getCamerasData() {
        File file = new File("${grailsApplication.config.camerasHomeDirectory}/cameras.json")
        JsonSlurper parser = new JsonSlurper()
        def json = parser.parse(file)
        return json as Map<String, Camera>
    }
}
