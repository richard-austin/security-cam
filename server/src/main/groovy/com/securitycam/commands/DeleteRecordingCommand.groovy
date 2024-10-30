package com.securitycam.commands

import com.securitycam.controllers.Stream

class DeleteRecordingCommand {
    Stream stream   // The camera stream that the files are recordings from
    String fileName  // The name of one of the files in the recording

    // Not restful argument, this is set up by the validator as the
    //  wildcard for matching all the files in the recording
    String epoch

    // Not a restful argument, this is set to the location that the recording will be deleted from
    File folder
}
