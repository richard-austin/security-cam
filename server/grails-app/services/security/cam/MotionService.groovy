package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.commands.DeleteRecordingCommand
import security.cam.commands.DownloadRecordingCommand
import security.cam.commands.GetMotionEventsCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import org.apache.commons.io.FilenameUtils

import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Transactional
class MotionService {
    GrailsApplication grailsApplication
    LogService logService
    UtilsService utilsService

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

    /**
     * downloadRecording: Convert the recording to a single .mp4 file and provide for download
     * @param cmd: manifest: The manifest file name.
     *             camera: The camera the recording is from
     * @return: Binary stream for the .mp4 file
     */
    ObjectCommandResponse downloadRecording(DownloadRecordingCommand cmd)
    {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try
        {
            String fileBaseName = FilenameUtils.removeExtension(cmd.manifest)

            Path pathToHls = Paths.get(cmd.folder.toString(), cmd.manifest)
            Path pathToMp4 = Paths.get(cmd.folder.toString(), fileBaseName+".mp4")
            // Create a single .mp4 file from the hls file set
            utilsService.executeLinuxCommand("ffmpeg -y  -i ${pathToHls.toString()} -c copy -level 3.0  -f mp4 ${pathToMp4.toString()}")

            File mp4File = new File(pathToMp4.toString())
            if(!mp4File.exists())
            {
                result.status = PassFail.FAIL
                result.error = "Could not create file ${fileBaseName}.mp4 for download"
            }
            else
                result.responseObject = mp4File
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in downloadRecording: "+ex.getCause()+ ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }
}
