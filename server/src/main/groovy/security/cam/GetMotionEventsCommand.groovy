package security.cam
import grails.core.GrailsApplication
import grails.validation.Validateable
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class GetMotionEventsCommand implements Validateable{
    String cameraName
    String uri

    GrailsApplication grailsApplication
    CamService camService

    static constraints = {
        cameraName(nullable: true, size: 1..50,
            validator: { cameraName, cmd ->
                def cameraNames = cmd.grailsApplication.config.motion.cameraNames

                def result = cameraNames.find{
                    it['name'] == cameraName
                }

                if(result == null && cameraName != null)
                    return "Camera name ${cameraName} not known"
            }
        )

        uri(nullable: false, size: 2..200,
            validator: {uri, cmd ->
                ObjectCommandResponse camRespObj = cmd.camService.getCameras()
                if(camRespObj.status != PassFail.PASS)
                    return "An error occurred when getting camera details"
                def cams = camRespObj.responseObject

                boolean uriValid = false
                cams.each{ cam ->
                    cam.value?.recordings?.each{recording ->
                        if(recording.uri == uri)
                            uriValid = true
                    }
                }

                if(!uriValid)
                    return "uri ${uri} not knowm"
            }
        )
    }
}
