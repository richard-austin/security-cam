package security.cam

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import org.grails.web.json.JSONObject
import org.springframework.messaging.simp.SimpMessagingTemplate
import security.cam.commands.ChangeEmailCommand
import security.cam.commands.CreateOrUpdateAccountCommand
import security.cam.commands.ResetPasswordCommand
import security.cam.commands.ResetPasswordFromLinkCommand
import security.cam.commands.SendResetPasswordLinkCommand
import security.cam.commands.SetupGuestAccountCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

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
import java.util.concurrent.ConcurrentHashMap

@Transactional()
class UserAdminService {
    SpringSecurityService springSecurityService
    LogService logService

    UserService userService
    UtilsService utilsService
    UserRoleService userRoleService
    RoleService roleService
    SimpMessagingTemplate brokerMessagingTemplate

    final String logoff = new JSONObject()
            .put("message", "logoff")
            .toString()


    ObjectCommandResponse resetPassword(ResetPasswordCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def principal = springSecurityService.getPrincipal()
            String userName = principal.getUsername()

            User user = User.findByUsername(userName)
            user.setPassword(cmd.getNewPassword())
            user.save()
        }
        catch (Exception ex) {
            logService.cam.error("Exception in resetPassword: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    def resetPasswordFromLink(ResetPasswordFromLinkCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            String userName
            def users = User.findAll()
            users.forEach {user ->
                def auths = user.getAuthorities()
                auths.forEach {role ->
                    if(role.authority == 'ROLE_CLIENT') {
                        user.setPassword(cmd.getNewPassword())
                        user.save()
                        return result
                    }
                }
            }
        }
        catch (Exception ex) {
            logService.cam.error("Exception in resetPassword: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    /**
     * createAccount: This is used to create a local web account on the NVR which can be logged into directly, rather
     *                than via the Cloud service.
     * @param cmd :     String username
     *                 String password
     *                 String confirmPassword
     *                 String email
     *                 String confirmEmail
     *
     * @return: Success/error status
     */
    ObjectCommandResponse createOrUpdateAccount(CreateOrUpdateAccountCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            if(cmd.updateExisting)
            {
                def roleClient = roleService.findByAuthority('ROLE_CLIENT')
                def u = User.all.find {
                    def auth = it.getAuthorities()
                    return auth.size() == 1 && auth[0] == roleClient
                }
                u.username = cmd.username
                u.password = cmd.password
                u.email = cmd.email
                userService.save(u)
              //  userRoleService.save(u, roleService.findByAuthority('ROLE_CLIENT'))
            }
            else {
                User u = new User(username: cmd.username, password: cmd.password, email: cmd.email, cloudAccount: false, header: null)
                u = userService.save(u)
                userRoleService.save(u, roleService.findByAuthority('ROLE_CLIENT'))
            }

        }
        catch (Exception ex) {
            logService.cam.error("Exception in createAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    /**
     * removeAccount: Remove the local NVR direct web access account
     * @return Success/error status
     */
    ObjectCommandResponse removeAccount() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            User user = User.all.find { it.username != 'guest' && !it.cloudAccount }
            if (user != null) {
                result.responseObject = [username: user.getUsername()]
                UserRole userRole = UserRole.findByUser(user)
                userRole.delete(flush: true)
                user.delete(flush: true)

                // Disable the guest account
                user = User.findByUsername('guest')
                user.setEnabled(false)
                userService.save(user)
            } else
                throw new Exception("There is no local web account present on this NVR")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in removeAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse hasLocalAccount() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            result.responseObject = User.all.find { it.username != 'guest' && !it.cloudAccount } != null
        }
        catch (Exception ex) {
            logService.cam.error("Exception in hasLocalAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse setupGuestAccount(SetupGuestAccountCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User u = User.all.find { it.username == 'guest' && !it.cloudAccount }
            u.enabled = cmd.enabled
            if (cmd.password != "" && cmd.password != null) {
                u.setPassword(cmd.password)
            }

            u.passwordExpired = false
            userService.save(u)

            // Kick off any guest users who are logged in
            if(!cmd.enabled)
                brokerMessagingTemplate.convertAndSend("/topic/logoff", logoff)
        }
        catch (Exception ex) {
            logService.cam.error("Exception in setupGuestAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse getEmail() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def principal = springSecurityService.getPrincipal()
            String userName = principal.getUsername()

            User user = User.findByUsername(userName)

            result.responseObject = [email: user.getEmail()]
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getEmail: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse changeEmail(ChangeEmailCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            def principal = springSecurityService.getPrincipal()
            String userName = principal.getUsername()

            User user = User.findByUsername(userName)
            user.setEmail(cmd.getNewEmail())
            user.save()
        }
        catch (Exception ex) {
            logService.cam.error("Exception in changeEmail: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse isGuest() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            boolean isGuest = false
            def principal = springSecurityService.getPrincipal()
            if(principal) {
                String userName = principal.getUsername()

                User user = User.findByUsername(userName)

                if (user) {
                    Set<Role> auths = user.getAuthorities()

                    auths.forEach { role ->
                        if (role.authority == 'ROLE_GUEST')
                            isGuest = true
                    }
                }
            }
            result.responseObject = [guestAccount: isGuest]
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in isGuest: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse guestAccountEnabled()
    {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User guest = User.all.find{it.username == "guest" && !it.cloudAccount}
            result.responseObject = [enabled: guest != null && guest.enabled]
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in guestAccountEnabled: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    final private resetPasswordParameterTimeout = 20 * 60 * 1000 // 20 minutes
    final private Map<String, String> passwordResetParameterMap = new ConcurrentHashMap<>()
    final private Map<String, Timer> timerMap = new ConcurrentHashMap<>()

    ObjectCommandResponse sendResetPasswordLink(SendResetPasswordLinkCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            String uniqueId = generateRandomString()
            // There should only ever be one entry in the passwordResetParameterMap and timerMap
            if(passwordResetParameterMap.size() > 0) {
                passwordResetParameterMap.clear()
                timerMap.forEach {ignore, timer ->
                    timer.cancel()
                }
                timerMap.clear()
            }

            passwordResetParameterMap.put(uniqueId, cmd.email)
            ResetPasswordParameterTimerTask task = new ResetPasswordParameterTimerTask(uniqueId, passwordResetParameterMap, timerMap)
            Timer timer = new Timer(uniqueId)
            timer.schedule(task, resetPasswordParameterTimeout)
            timerMap.put(uniqueId, timer)

            sendResetPasswordEmail(cmd.getEmail(), uniqueId, cmd.getClientUri())
        }
        catch(Exception ex)
        {
            logService.cam.error("${ex.getClass().getName()} in sendResetPasswordLink: ${ex.getCause()} ${ex.getMessage()}")
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    private static String generateRandomString() {
        int leftLimit = 48 // numeral '0'
        int rightLimit = 122 // letter 'z'
        int targetStringLength = 212
        Random random = new Random()

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
        System.out.println(generatedString)
        return generatedString
    }

    private def sendResetPasswordEmail(String email, String idStr, String clientUri)
    {
        User user = User.findByEmail(email)
        if(user != null) {
            def auths = user.getAuthorities()
            boolean isClient = false
            auths.forEach { role ->
                if (role.authority == 'ROLE_CLIENT')
                    isClient = true
            }
            if (isClient) {   // Correct email name and role is ROLE_CLIENT
                //noinspection DuplicatedCode
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
                        return new PasswordAuthentication(smtpData.username, smtpData.password)
                    }
                })

                Message message = new MimeMessage(session)
                message.setFrom(new InternetAddress(smtpData.fromAddress))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                message.setSubject("Reset Password")

                String msg = "<h2>Reset Password for user: ${user.username}</h2>" +
                        "An NVR password reset link was requested. If this was not you, please ignore this email.<br> " +
                        "Please click <a href=\"" + clientUri + "/recover/resetPasswordForm?key=${idStr}\">here</a> to reset your NVR password."

                MimeBodyPart mimeBodyPart = new MimeBodyPart()
                mimeBodyPart.setContent(msg, "text/html; charset=utf-8")

                Multipart multipart = new MimeMultipart()
                multipart.addBodyPart(mimeBodyPart)

                message.setContent(multipart)

                Transport.send(message)
            }
        }
        // No error if email address was wrong, just ignore it
    }
}
