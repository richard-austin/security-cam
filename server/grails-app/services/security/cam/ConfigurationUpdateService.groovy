package security.cam

import grails.gorm.transactions.Transactional
import org.apache.commons.io.FileUtils
import org.apache.tools.ant.taskdefs.Sleep
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import server.Camera

@Transactional
class ConfigurationUpdateService {
    CamService camService
    Sc_processesService sc_processesService
    LogService logService

    def generateConfigs() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try
        {
            // Stop sc_processes.sh before updating the config
            sc_processesService.stopProcesses()
            Thread.sleep(300)  // Give it time to stop

            // Clear out the existing motion config files before we regenerate them.
            FileUtils.cleanDirectory(new File("/home/security-cam/motion/conf.d"))  // TODO: Must change these hard coded references to configured ones.

            response = camService.getCameras()

            Map<String, Camera> jsonObj = response.responseObject as Map<String, Camera>
            int camId = 100
            int camNum = 1
            jsonObj.each { it ->
                Camera cam = it.value
                if(cam.motion != null) {
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
camera_name ${cam.motion.trigger_recording_on}

# Mask to exclude public areas
${cam.motion?.mask_file ? "mask_file $cam.motion.mask_file" : "; mask_file"}

# Numeric identifier for the camera.
camera_id ${++camId}

# The full URL of the network camera stream.
netcam_url ${cam.netcam_uri}
# netcam_url rtmp://localhost/livelo/porch

# Image width in pixels.
width $cam.video_width

# Image height in pixels.
height $cam.video_height

# Text to be overlayed in the lower left corner of images
text_left $cam.name

# Target directory for pictures, snapshots and movies
target_dir /home/security-cam/$cam.recording.location

# File name(without extension) for movies relative to target directory
movie_filename ${camNum < 10 ? "cam0" : "cam"}${camNum}_%t-%v-%s

framerate 3

movie_extpipe_use off
; movie_extpipe ffmpeg -y -f rawvideo -pix_fmt yuv420p -video_size %wx%h -framerate %fps -i pipe:0 -vcodec libx264 -preset ultrafast -level 3.0 -start_number 0 -hls_time 5.0 -hls_list_size 0 -f hls %f.m3u8

movie_passthrough on
; movie_codec mp4
"""
                    FileWriter writer = new FileWriter("/home/security-cam/motion/conf.d/${it.key}.conf") //TODO: Must change these hard coded references to configured ones.
                    writer.write(motionConf)
                    writer.close()
                }
                ++camNum
            }

           // Update sc_processes.sh
            UpdateScProcesses(jsonObj)
            sc_processesService.startProcesses()
            Thread.sleep(300)  // Give it time to stop
        }
        catch (Exception ex)
        {
            logService.cam.error "Exception in generateConfigs: " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }

        return response
    }

    /**
     * UpdateScProcesses: Create a new sc_processes.sh file from the cameras.json file and replace any existing one with it
     */
    static private UpdateScProcesses(Map<String, Camera> jsonObj )
    {
        // Remove the current sc_processes.sh file
//        File scProcs = new File("/etc/security-cam/sc_processes.sh") // TODO: Must change these hard coded references to configured ones.
//        scProcs.delete()


        String scProcsTxt =
                """#!/bin/bash

log_dir=/home/security-cam/logs/
ipV4RegEx="^([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\$"

read_ip() {
    read -r last_ip </home/security-cam/myip

    current_reading=\$(curl -s 'https://api.ipify.org/?format=json' | python3 -c "import sys, json; print(json.load(sys.stdin)['ip'])")

    if [[ \$current_reading =~ \$ipV4RegEx  ]]; then
      current_ip=\${current_reading}
    else
      echo "\$(date +%d-%m-%Y" "%T): Bad reading (\${current_reading}) from https://api.ipify.org" >>"\${log_dir}ipify_\$(date +%Y%m%d)".log
    fi

    # The myip file is updated when the user uses the Save Current IP option in the web application
    #  in response to the email sent here
}

kill_descendant_processes() {
    local pid="\$1"
    local and_self="\${2: -false}"
    if children="\$(pgrep -P "\$pid")"; then
        for child in \$children; do
            kill_descendant_processes "\$child" true
        done
    fi
    if [[ "\$and_self" == true ]]; then
        kill -TERM "\$pid"
    fi
}

run_check_ip_not_changed() {
  while true; do
    sleep 15m
    read_ip

    while [ "\$current_ip" != "\$last_ip" ]; do
      ## Send the email with the ssmtp command
      ssmtp richard.david.austin@gmail.com <<EOT
From: "Raspberry pi" <rdaustin@virginmedia.com>
Subject: Change of public IP address

Hi Richard,

I have detected a change of Virgin Media broadband IP address, this is now https://\${current_ip}

Please go to the web application at the new address and use the "Save Current Public IP" option  on the General menu to stop these emails continuing to be sent.

Thanks

Raspberry pi
EOT
      sleep 60m
      read_ip
    done
  done
}

run_ffmpeg() {
  while true; do
    /usr/bin/ffmpeg -hide_banner -loglevel error -stimeout 1000000 -rtsp_transport tcp -i "\$1" -an -c copy -f flv rtmp://localhost/"\$2" 2>>\${log_dir}ffmpeg_"\$3"_"\$(date +%Y%m%d)".log
    sleep 1
    # ffmpeg -hide_banner -loglevel error -stimeout 1000000 -re -rtsp_transport tcp -i \$1 -c copy -c:a aac -b:a 160k -ar 44100 -f flv rtmp://localhost/\$2/\$3 2>> \${log_dir}ffmpeg_\$2_\$3_`date +%Y%m%d`.log
    echo "ffmpeg terminated at \$(date +%d-%m-%Y" "%T)" >>"\${log_dir}ffmpeg_\$2_\$3_\$(date +%Y%m%d)".log
  done
}

run_nms() {
  while true; do
    /usr/bin/node /etc/security-cam/nms/app.js
    sleep 1
  done
}

run_motion() {
  while true; do
    /usr/bin/motion
    sleep 1
  done
}

run_nms &
run_motion &
"""

        // Add an ffmpeg call for each camera stream
        jsonObj.each { it ->
            Camera cam = it.value

            scProcsTxt +=
                    "run_ffmpeg $cam.netcam_uri $cam.nms_uri \"${cam.name.replace(" ", "_")+"_"+cam.descr.replace(" ", "_")}\" &\n"
        }

        scProcsTxt +=
                """
run_check_ip_not_changed &

trap 'kill_descendant_processes \$\$' INT EXIT TERM
wait
"""
        FileWriter writer = new FileWriter("/home/security-cam/sc_processes.sh") //TODO: Must change these hard coded references to configured ones.
        writer.write(scProcsTxt)
        writer.close()

    }
}
