package com.securitycam.controllers

import com.securitycam.services.LogService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import java.nio.file.Files

@RestController
@RequestMapping("/recording")
class RecordingFeedController {
    LogService logService

    @Autowired
    RecordingFeedController(LogService logService) {
        this.logService = logService
    }

    @RequestMapping("/**")
    def getRecordingFile(HttpServletRequest request, HttpServletResponse response) {
        URI uri = new URI(request.requestURL.toString())
        String[] sections = uri.getPath().split("/")
        String controller = sections[sections.length - 3]

        String location = sections[sections.length - 2]
        if (controller == 'recording' && location.startsWith("rec")) {
            try {
                String fileName = sections[sections.length - 1]
                File file = new File("/var/security-cam/$location/$fileName")
                byte[] bytes = Files.readAllBytes(file.toPath())

                response.setHeader("Accept-Ranges", "bytes")
                if(fileName.endsWith(".m3u8"))
                    response.setHeader("content-type", "application/vnd.apple.mpegurl")
                else if (fileName.endsWith(""))
                    response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                else if(fileName.endsWith(".mp4"))
                    response.setHeader("content-type", "video/mp4")
                response.setContentLength(bytes.length)
                response.getOutputStream().write(bytes)
            }
            catch (Exception ex) {
                logService.cam.error("${ex.getClass().getName()} in RecordingFeedController getRecordingFile: ${ex.getMessage()}")
                return ResponseEntity.internalServerError().body(ex.getMessage())
            }

        } else {
            return ResponseEntity.badRequest().body("Malformed URL ${uri.getPath()}")
        }
    }
}
