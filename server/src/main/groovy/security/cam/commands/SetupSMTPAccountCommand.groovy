package security.cam.commands

import grails.validation.Validateable
import security.cam.UserAdminService
import security.cam.UtilsService

class SMTPData {
    boolean auth
    String username
    String password
    String confirmPassword
    boolean enableStartTLS
    String sslProtocols
    String sslTrust
    String host
    int port
    String fromAddress
}

class SetupSMTPAccountCommand implements Validateable {
    UtilsService utilsService
    boolean auth
    String username
    String password
    String confirmPassword
    boolean enableStartTLS
    String sslProtocols
    String sslTrust
    String host
    int port
    String fromAddress
    UserAdminService userAdminService

    SMTPData getData() {
        return new SMTPData(auth: auth,
                username: username,
                password: password,
                enableStartTLS: enableStartTLS,
                sslProtocols: sslProtocols,
                sslTrust: sslTrust,
                host: host,
                port: port,
                fromAddress: fromAddress)
    }

    static constraints = {
        auth(nullable: false, inList: [true, false],
        validator: {auth, cmd ->
            def response = cmd.userAdminService.isGuest()
            if(response.responseObject.guestAccount)
                return "Guest not authorised to administer SMTP client account"
        })
        username(nullable: true,
                validator: { username, cmd ->
                    if (cmd.auth) {
                        if (username == null || username == "")
                            return "username is required"
                        else if (username.size() > 50)
                            return "Maximum username length is 50 characters"
                    }
                    return
                })
        password(nullable: true,
                validator: { password, cmd ->
                    if (cmd.auth) {
                        if (password == null || password == "")
                            return "password is required"
                        else if (password.size() > 50)
                            return "Maximum password length is 50 characters"
                    }
                    return
                })
        confirmPassword(nullable: true,
                validator: { confirmPassword, cmd ->
                    if (cmd.auth) {
                        if (confirmPassword != cmd.password)
                            return "password and confirmPassword must match"
                    }
                    return
                })
        enableStartTLS(nullable: false, inList: [true, false])
        sslProtocols(validator: { sslProtocols, cmd ->
            if (cmd.enableStartTLS) {
                if (sslProtocols != "TLSv1.2" && sslProtocols != "TLSv1.3") {
                    return "sslProtocols should be TLSv1.2 or TLSv1.3 if enableStartTLS is true"
                }
                return
            }
        })
        sslTrust(nullable: true,
                validator: { sslTrust, cmd ->
                    if (cmd.enableStartTLS) {
                        if (sslTrust == null || sslTrust == "") {
                            return "sslTrust is required if enableStartTLS is true"
                        }
                        return
                    }
                })
        host(nullable: false, blank: false, minSize: 3, maxSize: 50)
        port(nullable: false, min: 1, max: 65535)
        fromAddress(nullable: false, blank: false,
                validator: { fromAddress, cmd ->
                    if (!fromAddress.matches(cmd.utilsService.emailRegex))
                        return "Email format is not valid"

                })
    }
}
