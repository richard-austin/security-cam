package security.cam

import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.apache.commons.io.FileUtils
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import server.Camera
import server.Stream

@Transactional
class ConfigurationUpdateService {
    CamService camService
    Sc_processesService sc_processesService
    LogService logService

    /**
     * generateConfigs: Update all configuration files from the current cameras.json file. (cameras_dev.json in development mode)
     * @return ObjectCommandResponse indicating success or failure.
     */
    def generateConfigs() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        logService.cam.info "generateConfigs called"
        try {
            // Stop sc_processes.sh before updating the config
            sc_processesService.stopProcesses()

            int retryCount = 200
            while (sc_processesService.isRunning()) {
                if (--retryCount <= 0)
                    throw new Exception("Unable to stop sc_processes service")

                Thread.sleep(20)
            }

            Thread.sleep(300)  // Give it time to stop

            // Clear out the existing motion config files before we regenerate them.
            FileUtils.cleanDirectory(new File("/home/security-cam/motion/conf.d"))
            // TODO: Must change these hard coded references to configured ones.

            response = camService.getCameras()
            if(response.status == PassFail.PASS) {

                Map<String, Camera> jsonObj = response.responseObject as Map<String, Camera>

                logService.cam.info "Calling UpdateMotionConfig"
                UpdateMotionConfig(jsonObj)

                response = sc_processesService.startProcesses()
                Thread.sleep(300)  // Give it time to start
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
    static private UpdateMotionConfig(Map<String, Camera> jsonObj) {
        int camId = 100
        int streamNum = 1
        jsonObj.each { it ->
            Camera cam = it.value
            cam.streams.each { streamIt ->
                Stream stream = streamIt.value
                // TODO: Use paths from config instead of the hard coded paths used here
                if (stream.motion.enabled) {
                    String motionConf =
                            """
# /home/security-cam/motion/conf.d/${it.key}.conf
#
# This config file was generated by security-cam

###########################################################
# Configuration options specific to camera 1
############################################################

# This name is the key for the data of the camera on which a recording will be triggered.
#  Normally only the lower definition stream would be connected to motion to keep CPU utilisation
#  low, and this information enables motion to trigger a recording on the HD stream using the
#  the start_hd_recording.sh script
camera_name ${stream.motion.trigger_recording_on}

# Mask to exclude public areas
${stream.motion.mask_file != '' ? "mask_file /home/security-cam/motion/$stream.motion.mask_file" : "; mask_file"} 

# Numeric identifier for the camera.
camera_id ${++camId}

# The full URL of the network camera stream.
netcam_url ${stream.netcam_uri}
# netcam_url rtmp://localhost/livelo/porch

# Image width in pixels.
width $stream.video_width

# Image height in pixels.
height $stream.video_height

# Text to be overlayed in the lower left corner of images
text_left $cam.name

# Target directory for pictures, snapshots and movies
target_dir /home/security-cam/$stream.recording.location

# File name(without extension) for movies relative to target directory
movie_filename ${streamNum < 10 ? "cam0" : "cam"}${streamNum}_%t-%v-%s

framerate 3

movie_extpipe_use off
; movie_extpipe ffmpeg -y -f rawvideo -pix_fmt yuv420p -video_size %wx%h -framerate %fps -i pipe:0 -vcodec libx264 -preset ultrafast -level 3.0 -start_number 0 -hls_time 5.0 -hls_list_size 0 -f hls %f.m3u8

movie_passthrough on
; movie_codec mp4
"""
                    FileWriter writer = new FileWriter("/home/security-cam/motion/conf.d/${it.key}.conf")
                    //TODO: Must change these hard coded references to configured ones.
                    writer.write(motionConf)
                    writer.close()
                }
                ++streamNum
            }
        }

    }
}
