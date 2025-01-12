package com.securitycam.validators

import com.securitycam.commands.DeleteRecordingCommand
import com.securitycam.configuration.Config
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.nio.file.Path
import java.nio.file.Paths

class DeleteRecordingCommandValidator implements Validator {
    private final Config config

    DeleteRecordingCommandValidator(Config config) {
        this.config = config
    }

    @Override
    boolean supports(Class<?> clazz) {
        return DeleteRecordingCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (target instanceof DeleteRecordingCommand) {

            // Validate fileName and set up epoch
            def cmd = target

            if(cmd.fileName == null || cmd.fileName == "")
                errors.rejectValue("fileName", "fileName cannot be null or blank")
            else {
                // The file name should contain an epoch time somewhere in the middle preceded by a - and
                //  with an _ immediately following
                Integer dashIndex = cmd.fileName.lastIndexOf('-')
                Integer usIndex = cmd.fileName.lastIndexOf('_')
                if (dashIndex == -1 || usIndex == -1 || dashIndex > usIndex)
                    errors.rejectValue("fileName", "${cmd.fileName} is not a valid recording file name")
                cmd.epoch = cmd.fileName.substring(dashIndex + 1, usIndex)
                if (!cmd.epoch.matches(/^[0-9]{10}$/))
                    errors.rejectValue("fileName", "${cmd.fileName} is not a valid recording file name, it does not contain a valid epoch time")

                // Validate stream and set up folder
                if (cmd.stream == null || cmd.stream.descr == "" || cmd.stream.recording == null || cmd.stream.recording.location == null || cmd.stream.recording.location == "")
                    errors.rejectValue("stream","No stream was specified for which to delete a recording")

                String baseDir = config.recordingsHomeDirectory

                Path recordingsDirectory = Paths.get(baseDir as String, cmd.stream.recording.location as String)
                cmd.folder = new File(recordingsDirectory.toString())
            }
        }
    }
}
