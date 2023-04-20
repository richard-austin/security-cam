package security.cam.commands

import grails.validation.Validateable

class SetupSMTPAccountCommand implements Validateable {
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

    static constraints = {
        auth(nullable: false, inList: [true, false])
        username(validator: {username, cmd ->
            if(cmd.auth) {
                if(username == null || username == "")
                    return "username is required"
                else if(username.size() > 50)
                    return "Maximum username length is 50 characters"
            }
            return
        })
        password(validator: {password, cmd ->
            if(cmd.auth) {
                if (password == null || password == "")
                    return "password is required"
                else if (username.size() > 50)
                    return "Maximum password length is 50 characters"
            }
            return
        })
        confirmPassword(validator: {confirmPassword, cmd ->
            if(cmd.auth) {
                if(confirmPassword != cmd.password)
                    return "password and confirmPassword must match"
            }
            return
        })
        enableStartTLS(nullable: false, inList: [true, false])
        sslProtocols(validator: { sslProtocols, cmd ->
            if(cmd.enableStartTLS) {
                if(sslProtocols != "TLS1.2" && sslProtocols != "TLS1.3") {
                    return "sslProtocols should be TLS1.2 or TLS1.3 if enableStartTLS is true"
                }
                return
            }
        })
        sslTrust(validator: {sslTrust, cmd ->
            if(cmd.enableStartTLS) {
                if(sslTrust == null || sslTrust == "") {
                    return "sslTrust is required if enableStartTLS is true"
                }
                return
            }
        })
    }
}
