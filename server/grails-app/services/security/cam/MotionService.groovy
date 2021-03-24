package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.commands.DeleteRecordingCommand
import security.cam.commands.GetMotionEventsCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Transactional
class MotionService {
    GrailsApplication grailsApplication
    LogService logService

     /**
     * Get the names of the motion event files
     * @param cameraName
     * @return
     */
    def getMotionEvents(GetMotionEventsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            def baseDir = grailsApplication.config.camerasHomeDirectory

            // Determine whether to get motion events for the main recordings or the motion recordings.
            String location = cmd.camera.recording.location

            Path motionEventsDirectory = Paths.get(baseDir as String, location)
            File f = new File(motionEventsDirectory.toString())

            // Keep only the entries for the given cameraName, or return all if it's null
            result.responseObject = f.list(new FilenameFilter() {
                @Override
                boolean accept(File file, String s) {
                    return s.endsWith('.m3u8')
                }
            })

            if (result.responseObject == null) {
                result.status = PassFail.FAIL
                result.error = "Cannot access motion events"
                logService.cam.error("Error in getMotionEvents: "+result.error)
            }
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getMotionEvents: "+ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    /**
     * deleteRecording: Delete all the files in the set for a recording
     * @param cmd: camera: The camera which the recording to be deleted is from
     *             fileName: The name of one of the files in the recording
     * @return
     */
    ObjectCommandResponse deleteRecording(DeleteRecordingCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try
        {
            File[] files = cmd.folder.listFiles(new FilenameFilter() {
                @Override
                boolean accept(File file, String name) {
                    return name.matches('.+'+cmd.epoch+'.+')
                }
            })

            for(File file : files)
                Files.delete(file.toPath())
        }
        catch(AccessDeniedException ex)
        {
            logService.cam.error("Access denied exception in getMotionEvents: "+ ex.getMessage())
            result.status = PassFail.FAIL
            result.error = "Cannot delete " + ex.getMessage() +", access denied"
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in getMotionEvents: "+ex.getCause()+ ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }
}
