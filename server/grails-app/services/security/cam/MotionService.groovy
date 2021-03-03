package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

@Transactional
class MotionService {
    GrailsApplication grailsApplication
    CamService camService

    private String getMasterManifest(GetMotionEventsCommand cmd)
    {
        String retVal = "";
        def cams = camService.getCameras().responseObject
        for (cam in cams)
        {
            def camera = cam.value

            for(recording in camera.recordings)
            {
                if(recording.uri == cmd.uri) {
                    retVal = recording.masterManifest
                    return retVal;
                }
            }
        }
        return retVal
    }

    private def getOffsetsToEvents(String masterManifest, ArrayList<Integer> eventTimes)
    {

    }

     /**
     * Get the names of the motion event files
     * @param cameraName
     * @return
     */
    def getMotionEvents(GetMotionEventsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            String motionEventsDirectory = grailsApplication.config.motion.motionEventsDirectory

            File f = new File(motionEventsDirectory)

            // Keep only the entries for the given cameraName, or return all if it's null
            result.responseObject = f.list(new FilenameFilter() {
                @Override
                boolean accept(File file, String s) {
                    if (cmd.cameraName == null)
                        return true
                    else
                        return s.startsWith(cmd.cameraName)
                }
            })

            if (result.responseObject == null) {
                result.status = PassFail.FAIL
                result.error = "Cannot access motion events"
            }
            else
            {
                String manifest = getMasterManifest(cmd)
                if(manifest == "") {
                    result.status = PassFail.FAIL
                    result.error = "Cannot get manifest file"
                }
                else
                {

                }
            }
        }
        catch (Exception ex) {
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }
}
