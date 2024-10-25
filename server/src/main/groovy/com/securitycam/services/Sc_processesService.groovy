package com.securitycam.services

import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.dao.UserRepository
import com.securitycam.enums.PassFail
import com.securitycam.eventlisteners.IpCheckTimerTask
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.RestfulResponse
import com.securitycam.model.User
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.transaction.annotation.Transactional

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@org.springframework.stereotype.Service
@Transactional
class Sc_processesService {
    @Autowired
    LogService logService
    @Autowired
    RestfulInterfaceService restfulInterfaceService
    @Autowired
    Config config
    @Autowired
    UtilsService utilsService

    OnvifService onvifService

    // Set from OnvifService
    void setOnvifService(OnvifService onvifService) {
        this.onvifService = onvifService
    }

    ArrayList<Process> processes
    UserRepository userRepository
    ExecutorService processExecutors

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
        boolean ipOk
        do {
            try (Scanner s = new Scanner(new URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
                retVal = s.next()
            } catch (IOException ex) {
                logService.cam.error "Exception in readMyIp: " + ex.getMessage()
            }
            // We were getting empty IP readings occasionally, so make sure to eliminate these
            ipOk = retVal != null && retVal.length() >= 6
        }
        while(!ipOk)
        return retVal
    }

    /**
     * getSavedIP: Get the IP address stored in the myip file.
     * @return
     */
    private String getSavedIP() {
        String retVal = ""
        File file = new File(config.myipFileLocation as String)

        try (Scanner reader = new Scanner(file)) {
            if (reader.hasNextLine())
                retVal = reader.nextLine()
        }
        catch (Exception ex) {
            logService.cam.error "${ex.getClass().getName()} in getSavedIP: " + ex.getMessage()
            logService.cam.info("Attempting to get the current IP and save to myIp")
            ObjectCommandResponse resp = utilsService.setIP()
            if (resp.status == PassFail.PASS)
                logService.cam.info("myIp set successfully")
            else
                logService.cam.error("myIp not set")

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
    protected void emailTask() {
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
                    User user = userRepository.findByUsernameNotAndCloudAccount("guest", false)
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
        def smtpData = utilsService.getSMTPConfigData()

        Properties prop = new Properties()
        prop.put("mail.smtp.auth", smtpData.auth)
        prop.put("mail.smtp.starttls.enable", smtpData.enableStartTLS)
        if(smtpData.enableStartTLS) {
            prop.put("mail.smtp.ssl.protocols", smtpData.sslProtocols)
            prop.put("mail.smtp.ssl.trust", smtpData.sslTrust)
        }
        prop.put("mail.smtp.host", smtpData.host)
        prop.put("mail.smtp.port", smtpData.port)

        logService.cam.trace("mail.smtp.auth=${smtpData.auth}")
        logService.cam.trace("mail.smtp.starttls.enable=${smtpData.enableStartTLS}")
        logService.cam.trace("mail.smtp.ssl.protocols=${smtpData.sslProtocols}")
        logService.cam.trace("mail.smtp.host=${smtpData.host}")
        logService.cam.trace("mail.smtp.port=${smtpData.port}")
        logService.cam.trace("mail.smtp.ssl.trust=${smtpData.sslTrust}")

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                logService.cam.trace("Authenticating: ${smtpData.username}:xxxxxxxxxx")
                return new PasswordAuthentication(smtpData.username, smtpData.password)
            }
        })
        session.setDebug(true)
        FileOutputStream fs = new FileOutputStream("/var/log/security-cam/javaxMailLog.log")
        PrintStream ps = new PrintStream(fs, true)
        session.setDebugOut(ps)
        Message message = new MimeMessage(session)
        logService.cam.trace("fromaddress: ${smtpData.fromAddress}")
        message.setFrom(new InternetAddress(smtpData.fromAddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientAddress))
        message.setSubject("Change of public IP address")

        String msg = """Dear NVR user.
<div>
    I have detected a change of broadband IP address, this is now ${currentIP}
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
     * startProcesses: Start the motion sensing, media server and the IP change detection processes
     * @return
     */
    def startProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        logService.cam.debug "startProcesses called"
        try {
            setupIpCheckTimer()
            running = true
            final int startServicesTimeout = 12000
            processExecutors = Executors.newCachedThreadPool()
             // Start motion and media servers and reload config to take on any changes
            RestfulResponse restResponse = restfulInterfaceService.sendRequest("localhost:8000", "/",
                    "{\"command\": \"start_services\"}",
                    true, startServicesTimeout)

            // Populate the onvif device map so the devices don't have to be created each time a PTZ operation is done
            onvifService.populateDeviceMap()

            if (restResponse.responseCode != 200) {
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
     * stopProcesses: Stop all started processes and their descendents
     */
    def stopProcesses() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            running = false
            final int stopServicesTimeout = 15000
            RestfulResponse restResponse = restfulInterfaceService.sendRequest("localhost:8000", "/",
                    "{\"command\": \"stop_services\"}",
                    true, stopServicesTimeout)
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
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in stopProcesses: ${ex.getMessage()}")
            response.status = PassFail.FAIL
            response.error = "${ex.getClass().getName()} in stopProcesses: ${ex.getMessage()}"
        }
        return response
    }

    private Map<String, Camera> getCamerasData() {
        File file = new File("${config.camerasHomeDirectory}/cameras.json")
        JsonSlurper parser = new JsonSlurper()
        def json = parser.parse(file)
        return json as Map<String, Camera>
    }
}
