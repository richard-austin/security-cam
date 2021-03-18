package security.cam.commands

import grails.core.GrailsApplication
import grails.validation.Validateable
import server.Camera

import java.nio.file.Path
import java.nio.file.Paths

class DeleteRecordingCommand  implements Validateable{
    GrailsApplication grailsApplication
    Camera camera   // The camera that the files are recordings from
    String fileName  // The name of one of the fies in the recording

    // Not restful argument, this is set up by the validator as the
    //  wildcard for matching all the files in the recording
    String epoch

    // Not a restful argument, this is set to the location that the recording will be deleted from
    File folder

    static constraints = {
        camera(nullable: false,
                validator: { camera, cmd ->
                    if (camera.name == null || camera.name == "")
                        return "No camera was specified for which to delete a recording"

                    String baseDir = cmd.grailsApplication.config.camerasHomeDirectory
                    Path recordingsDirectory = Paths.get(baseDir as String, cmd.camera.recording.location as String)
                    cmd.folder = new File(recordingsDirectory.toString())
                    return
                })

        fileName(nullable: false, blank: false,
                validator:{fileName, cmd ->
                    // The file name should contain an epoch time somewhere in the middle preceded by a - and
                    //  with an _ immediately following
                    Integer dashIndex = fileName.lastIndexOf('-')
                    Integer usIndex = fileName.lastIndexOf('_')
                    if(dashIndex == -1 || usIndex == -1 || dashIndex > usIndex)
                        return "${fileName} is not a valid recording file name"
                    cmd.epoch = fileName.substring(dashIndex+1, usIndex)
                    if(!cmd.epoch.matches(/^[0-9]{10}$/))
                        return "${fileName} is not a valid recording file name, it does not contain a valid epoch time"
                 })
    }
}
