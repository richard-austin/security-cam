package com.securitycam.commands

import com.securitycam.controllers.Stream
class DownloadRecordingCommand {
    Stream stream
    String manifest

    // Not a restful argument, this is set to the location that the recording will be downloaded from
    File folder
}
