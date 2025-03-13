package com.securitycam.services

import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.controllers.Stream
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

@Transactional
@Service
class ConfigurationUpdateService {
    @Autowired
    CamService camService
    @Autowired
    Sc_processesService sc_processesService
    @Autowired
    LogService logService
    @Autowired
    Config config

    /**
     * generateConfigs: Update all configuration files from the current cameras.json file. (cameras_dev.json in development mode)
     * @return ObjectCommandResponse indicating success or failure.
     */
    def generateConfigs() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        logService.cam.info "generateConfigs called"
        try {
            int retryCount = 200
            while (sc_processesService.isRunning()) {
                if (--retryCount <= 0)
                    throw new Exception("Unable to stop sc_processes service")

                Thread.sleep(20)
            }

            Thread.sleep(300)  // Give it time to stop
            final String motionConfigDir = config.motion.configDir

            // Clear out the existing motion config files before we regenerate them.
            FileUtils.cleanDirectory(new File(motionConfigDir))

            response = camService.getCameras()
            if (response.status == PassFail.PASS) {

                Map<String, Camera> jsonObj = response.responseObject as Map<String, Camera>

                logService.cam.info "Calling UpdateMotionConfig"
                UpdateMotionConfig(jsonObj)
            }
        }
        catch (Exception ex) {
            logService.cam.error "Exception in generateConfigs: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }

        return response
    }

    /**
     * UpdateMotionConfig: Replace the motion config files with new ones
     *                     generated from the current cameras.json file.
     * @param jsonObj : json object created from the cameras.json file
     * @return: void
     */
    private UpdateMotionConfig(Map<String, Camera> jsonObj) {
        int camId = 100
        int streamNum = 1
        final String motionConfigDir = config.motion.configDir
        final String motionMaskFileDir = config.motion.maskFileDir
        final String motionRecordingDir = config.motion.recordingDir

        jsonObj.each { it ->
            Camera cam = it.value
            cam.streams.each { streamIt ->
                Stream stream = streamIt.value
                if (stream.recording.location != "") {
                    File f = new File(Paths.get(motionRecordingDir, stream.recording.location).toString())
                    f.mkdir()
                }
                if (stream.motion.enabled) {
                    String netcam_url = stream.netcam_uri
                    if (cam.cameraParamSpecs.camType == 0) {
                        def creds = cam.credentials()
                        def encoded_username = URLEncoder.encode(creds.userName as String, StandardCharsets.UTF_8.toString())
                        def encoded_password = URLEncoder.encode(creds.password as String, StandardCharsets.UTF_8.toString())
                        def idx = "rtsp://".length()
                        netcam_url = stream.netcam_uri.substring(0, idx) + encoded_username + ":" + encoded_password + "@" + stream.netcam_uri.substring(idx)
                    }
                    String motionConf =
                            """
# ${Paths.get(motionConfigDir, it.key).toString()}.conf
#
# This config file was generated by security-cam

###########################################################
# Configuration options specific to $stream.recording.location
############################################################

# This name is the key for the data of the camera on which a recording will be triggered.
#  Normally only the lower definition stream would be connected to motion to keep CPU utilisation
#  low, and this information enables motion to trigger a recording on the HD stream using the
#  with an http call via curl to the camera recording service
camera_name ${it.key}

# Threshold for number of changed pixels that triggers motion.
threshold ${stream.motion.threshold}

# Mask to exclude public areas
${stream.motion.mask_file != '' ? "mask_file ${Paths.get(motionMaskFileDir, stream.motion.mask_file).toString()}" : "; mask_file"} 

# Numeric identifier for the camera.
camera_id ${++camId}

# The full URL of the network camera stream.
netcam_url ${netcam_url}

# Image width in pixels.
width $stream.video_width

# Image height in pixels.
height $stream.video_height

# Text to be overlayed in the lower left corner of images
text_left $cam.name

# Target directory for pictures, snapshots and movies
target_dir ${Paths.get(motionRecordingDir, stream.recording.location).toString()}

# File name(without extension) for movies relative to target directory
movie_filename ${streamNum < 10 ? "cam0" : "cam"}${streamNum}_%t-%v-%s

framerate 3

movie_extpipe_use off
; movie_extpipe ffmpeg -y -f rawvideo -pix_fmt yuv420p -video_size %wx%h -framerate %fps -i pipe:0 -vcodec libx264 -preset ultrafast -level 3.0 -start_number 0 -hls_time 5.0 -hls_list_size 0 -f hls %f.m3u8

movie_passthrough on
; movie_codec mp4
"""
                    FileWriter writer = new FileWriter("${Paths.get(motionConfigDir, it.key).toString()}.conf")
                    writer.write(motionConf)
                    writer.close()
                }
                ++streamNum
            }
        }
    }
}
