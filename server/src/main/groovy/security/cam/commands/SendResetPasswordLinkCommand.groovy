package security.cam.commands

import grails.validation.Validateable
import security.cam.UtilsService

class SendResetPasswordLinkCommand implements Validateable {
    String email
    String clientUri

    UtilsService utilsService

    static constraints = {
        email(nullable: false, blank: false,
                validator: { email, cmd ->
                    if (!email.matches(cmd.utilsService.emailRegex))
                        return "Invalid email address"
                })
        clientUri(nullable: false, blank: false,
                validator: { clientUri ->
                    try {
                        new URI(clientUri)
                    }
                    catch (URISyntaxException ignored) {
                        return "Badly formed URL"
                    }
                    return
                })
    }
}
