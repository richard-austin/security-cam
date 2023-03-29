package security.cam.commands

import com.proxy.IResetTimerCommand
import grails.validation.Validateable

class ResetTimerCommand implements Validateable, IResetTimerCommand {
    String accessToken
    static constraints = {
        accessToken(nullable: false, blank: false)
    }
}
