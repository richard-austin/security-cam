package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.CloudProxyService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class CloudProxyController {
    CloudProxyService cloudProxyService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def start()
    {
        ObjectCommandResponse resp =  cloudProxyService.start()
        if(resp.status == PassFail.PASS)
            render (status: 200, text: "")
        else
            render(status: 500, text: resp.error)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_GUEST'])
    def isTransportActive() {
        ObjectCommandResponse resp =  cloudProxyService.isTransportActive()
        if(resp.status == PassFail.PASS)
            render (status: 200, text: [transportActive: resp.responseObject] as JSON)
        else
            render(status: 500, text: resp.error)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def stop()
    {
        ObjectCommandResponse resp = cloudProxyService.stop()
        if(resp.status == PassFail.PASS)
            render (status: 200, text: "")
        else
            render(status: 500, text: resp.error)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def restart()
    {
        ObjectCommandResponse resp = cloudProxyService.restart()
        if(resp.status == PassFail.PASS)
            render (status: 200, text: resp.responseObject as JSON)
        else
            render(status: 500, text: resp.error)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def status()
    {
        ObjectCommandResponse resp = cloudProxyService.status()
        if(resp.status == PassFail.PASS)
            render (status: 200, text: resp.responseObject)
        else
            render(status: 500, text: resp.error)
    }

}
