package com.securitycam.services

import com.securitycam.commands.GetMotionEventsCommand
import com.securitycam.configuration.Config
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.nio.file.Path
import java.nio.file.Paths

@Service
class MotionService {
    @Autowired
    LogService logService
    @Autowired
    UtilsService utilsService

    @Autowired
    Config config

     /**
     * Get the names of the motion event files
     * @param cameraName
     * @return
     */
    def getMotionEvents(GetMotionEventsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            def baseDir = config.getRecordingsHomeDirectory()

            // Determine whether to get motion events for the main recordings or the motion recordings.
            String location = cmd.stream.recording.location

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
}
