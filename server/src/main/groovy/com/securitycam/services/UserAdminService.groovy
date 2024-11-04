package com.securitycam.services

import com.google.gson.JsonObject
import com.securitycam.commands.AddOrUpdateActiveMQCredsCmd
import com.securitycam.commands.ChangeEmailCommand
import com.securitycam.commands.CreateOrUpdateAccountCommand
import com.securitycam.commands.ResetPasswordCommand
import com.securitycam.commands.ResetPasswordFromLinkCommand
import com.securitycam.commands.SendResetPasswordLinkCommand
import com.securitycam.commands.SetupGuestAccountCommand
import com.securitycam.dao.RoleRepository
import com.securitycam.dao.UserRepository
import com.securitycam.dto.UserDto
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.model.Role
import com.securitycam.model.User
import com.securitycam.proxies.CloudProxyProperties
import com.securitycam.timers.ResetPasswordParameterTimerTask
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

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

@Service
@Transactional
class UserAdminService {
    @Autowired
    LogService logService

    @Autowired
    UserRepository userRepository

    @Autowired
    RoleRepository roleRepository

    @Autowired
    UserService userService

    @Autowired
    UtilsService utilsService

    @Autowired
    PasswordEncoder passwordEncoder
    @Autowired
    SimpMessagingTemplate brokerMessagingTemplate

    @Autowired
    CloudProxyProperties cloudProxyProperties

    final String logoff = new JSONObject()
            .put("message", "logoff")
            .toString()


    ObjectCommandResponse resetPassword(ResetPasswordCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth.getPrincipal()
            if (principal) {
                String userName = auth.getName()

                User user = userRepository.findByUsername(userName)
                user.setPassword(passwordEncoder.encode(cmd.getNewPassword()))
                userRepository.save(user)
            }
            else
                throw new Exception("Could not get principal for this user")
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
            if (passwordResetParameterMap.containsKey(cmd.resetKey)) {
                clearPasswordResetKeyMapAndTimer()
                String userName
                def users = userRepository.findAll()
                users.forEach { user ->
                    def auths = user.getAuthorities()
                    auths.forEach { role ->
                        if (role.authority == 'ROLE_CLIENT') {
                            user.setPassword(cmd.getNewPassword())
                            userRepository.save()
                            return result
                        }
                    }
                }
            } else {
                logService.cam.error("Invalid password reset key")
                result.status = PassFail.FAIL
                result.error = "Invalid password reset key"
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
            Role role = roleRepository.findByName('ROLE_CLIENT')
            def roles = new ArrayList<Role>()
            if (role != null) {
                roles.push(role)

                if (cmd.updateExisting) {
                    def u = userRepository.findByRoles(roles)
                    userRepository.delete(u)
                }

                ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
                Validator validator = factory.getValidator()

                var accountDto = new UserDto(username: cmd.username, password: cmd.password, matchingPassword: cmd.confirmPassword, credentialsNonExpired: true, email: cmd.email, cloudAccount: false, role: role.getId())
                Set<ConstraintViolation<UserDto>> violations = validator.validate(accountDto)
                if (violations.size() == 0)
                    userService.registerNewUserAccount(accountDto)
                else
                    throw new ConstraintViolationException(violations)
            } else
                throw new Exception("Cannot find ROLE_CLIENT")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in createAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }


    ObjectCommandResponse addOrUpdateActiveMQCreds(AddOrUpdateActiveMQCredsCmd cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            cloudProxyProperties.setCloudCreds(cmd.username, cmd.password, cmd.mqHost)
        }
        catch(Exception ex) {
            logService.cam.error("Exception in createAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse hasLocalAccount() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            result.responseObject = userRepository.findByUsernameNotAndCloudAccount('guest', false)
        }
        catch (Exception ex) {
            logService.cam.error("Exception in hasLocalAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse isGuest() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            boolean isGuest = false
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth ? auth.getPrincipal() : null
            if (principal) {
                String userName = auth.getName()

                User user = userRepository.findByUsername(userName)

                if (user) {
                    Collection<GrantedAuthority> auths = user.getAuthorities()

                    auths.forEach { role ->
                        if (role.getAuthority() == 'ROLE_GUEST')
                            isGuest = true
                    }
                }
            }
            result.responseObject = [guestAccount: isGuest]
        }
        catch (Exception ex) {
            logService.cam.error("Exception in isGuest: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse guestAccountEnabled() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User guest = userRepository.findByUsernameAndCloudAccount("guest", false)
            result.responseObject = [enabled: guest != null && guest.enabled]
        }
        catch (Exception ex) {
            logService.cam.error("Exception in guestAccountEnabled: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
    ObjectCommandResponse getEmail() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth.getPrincipal()
            if(principal) {
                String userName = auth.getName()
                User user = userRepository.findByUsername(userName)
                result.responseObject = [email: user.getEmail()]
            }
            else
                throw new Exception("Could not get principal for this user")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getEmail: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse hasActiveMQCreds() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            JsonObject creds = cloudProxyProperties.getCloudCreds()
            final String mqHost = creds.get("mqHost")?.getAsString()
            result.responseObject = "{\"hasActiveMQCreds\": ${creds.get("mqUser").getAsString() != ""}, \"mqHost\": ${mqHost == null ? "\"<none>\"" : "\"$mqHost\""}}"
        }
        catch (Exception ex) {
            logService.cam.error("Exception in hasActiveMQCreds: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse setupGuestAccount(SetupGuestAccountCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            User u = userRepository.findByUsernameAndCloudAccount('guest', false)
            u.enabled = cmd.enabled
            if (cmd.password != "" && cmd.password != null) {
                u.setPassword(passwordEncoder.encode(cmd.password))

            }
            u.credentialsNonExpired = true
            userRepository.save(u)

            // Kick off any guest users who are logged in
            // TODO: This will also kick off non-guest users, it should be changed to specify guest users only
            if (!cmd.enabled)
                brokerMessagingTemplate.convertAndSend("/topic/logoff", logoff)
        }
        catch (Exception ex) {
            logService.cam.error("Exception in setupGuestAccount: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }


    ObjectCommandResponse changeEmail(ChangeEmailCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication()
            def principal = auth.getPrincipal()
            if(principal) {
                String userName = auth.getName()
                User user = userRepository.findByUsername(userName)
                user.setEmail(cmd.getNewEmail())
                userRepository.save(user)
            }
            else
                throw new Exception("Could not get principal for this user")
        }
        catch (Exception ex) {
            logService.cam.error("Exception in changeEmail: " + ex.getCause() + ' ' + ex.getMessage())
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

            User user = userRepository.findByEmail(cmd.email)
            if (user != null) {
                def isClient = false
                user.getAuthorities().forEach {role ->
                    if(role.authority == 'ROLE_CLIENT')
                        isClient = true
                }
                if(isClient) {
                    String uniqueId = generateRandomString()
                    // There should only ever be one entry in the passwordResetParameterMap and timerMap
                    if (passwordResetParameterMap.size() > 0) {
                        clearPasswordResetKeyMapAndTimer()
                    }

                    passwordResetParameterMap.put(uniqueId, cmd.email)
                    ResetPasswordParameterTimerTask task = new ResetPasswordParameterTimerTask(uniqueId, passwordResetParameterMap, timerMap)
                    Timer timer = new Timer(uniqueId)
                    timer.schedule(task, resetPasswordParameterTimeout)
                    timerMap.put(uniqueId, timer)

                    sendResetPasswordEmail(cmd.getEmail(), uniqueId, cmd.getClientUri())
                }
            }
        }
        catch (Exception ex) {
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

    private def sendResetPasswordEmail(String email, String idStr, String clientUri) {
        User user = userRepository.findByEmail(email)
        if (user != null) {
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
                if (smtpData.enableStartTLS) {
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

    private def clearPasswordResetKeyMapAndTimer() {
        passwordResetParameterMap.clear()
        timerMap.forEach { ignore, timer ->
            timer.cancel()
        }
        timerMap.clear()
    }

}
