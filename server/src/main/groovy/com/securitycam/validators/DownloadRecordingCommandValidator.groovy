package com.securitycam.validators

import com.securitycam.commands.DownloadRecordingCommand
import com.securitycam.configuration.Config
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.nio.file.Path
import java.nio.file.Paths

class DownloadRecordingCommandValidator implements Validator {
    DownloadRecordingCommand cmd
    Config config

    DownloadRecordingCommandValidator(Config config) {
        this.config = config
    }

    @Override
    boolean supports(Class<?> clazz) {
        return DownloadRecordingCommand == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if(target instanceof DownloadRecordingCommand) {
            cmd = target
            if (cmd.stream == null)
                errors.rejectValue("stream", "Stream cannot be null")
            else if (cmd.stream.descr == null || cmd.stream.descr == "" || cmd.stream.recording == null || cmd.stream.recording.location == null || cmd.stream.recording.location == "")
                errors.rejectValue("stream", "Stream must be correctly defined")
            String baseDir = /*config.*/ config.recordingsHomeDirectory

            Path recordingsDirectory = Paths.get(baseDir as String, cmd.stream.recording.location as String)
            cmd.folder = new File(recordingsDirectory.toString())
            Path manifestPath = Paths.get(recordingsDirectory.toString(), cmd.manifest)
            // Check that the manifest file exists
            File mf = new File(manifestPath.toString())
            if (!mf.exists())
                errors.rejectValue("manifest", "The manifest file ${cmd.manifest} does not exist")
            else if (mf.isDirectory())
                errors.rejectValue("manifest", "${cmd.manifest} is a directory")
        }
    }
}
