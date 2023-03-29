package security.cam.commands

import com.proxy.IGetAccessTokenCommand
import grails.validation.Validateable

class GetAccessTokenCommand implements Validateable, IGetAccessTokenCommand{
    String host
    int port

    static constraints = {
        host(nullable: false,
                validator: { host, GetAccessTokenCommand cmd ->
                    if (!host.matches(CameraParamsCommand.hostNameRegex) &&
                            !host.matches(CameraParamsCommand.ipV4RegEx) &&
                            !host.matches(CameraParamsCommand.ipV6RegEx))
                        return "Camera http host address format is invalid"
                    return
                })
        port(min: 1, max: 65535)
    }
}
