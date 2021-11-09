package security.cam.commands

import grails.core.GrailsApplication
import grails.validation.Validateable
import server.Stream

import java.nio.file.Path
import java.nio.file.Paths

class DownloadRecordingCommand implements Validateable {
    GrailsApplication grailsApplication
    Stream stream
    String manifest

    // Not a restful argument, this is set to the location that the recording will be downloaded from
    File folder

    static constraints = {
        stream(nullable: false,
                validator: { stream, cmd ->
                    if (stream== null || stream.descr == "")
                        return "No stream was specified for which to delete a recording"
                    return
                })

        manifest(nullable: false,
                validator: { manifest, cmd ->
                    String baseDir = cmd.grailsApplication.config.camerasHomeDirectory
                    Path recordingsDirectory = Paths.get(baseDir as String, cmd.stream.recording.location as String)
                    cmd.folder = new File(recordingsDirectory.toString())
                    Path manifestPath = Paths.get(recordingsDirectory.toString(), manifest)
                    // Check that the manifest file exists
                    File mf = new File(manifestPath.toString())
                    if (!mf.exists())
                        return "The manifest file ${manifest} does not exist"
                    else if (mf.isDirectory())
                        return "${manifest} is a directory"
                    return
                })
    }
}
