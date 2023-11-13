package main

import (
	"fmt"
	log "github.com/sirupsen/logrus"
	"net/url"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

func ffmpegFeed(config *Config, cameras *Cameras, creds *CameraCredentials) {
	go cleanLogs()
	path, _ := filepath.Split(config.LogPath)
	for _, camera := range cameras.Cameras {
		for _, stream := range camera.Streams {
			go func(camera Camera, stream StreamC) {
				for {
					time.Sleep(time.Second)
					var audio string
					if !stream.Audio {
						audio = "-an"
					} else if stream.AudioEncoding != "AAC" {
						audio = "-c:a aac"
					} else {
						audio = "-c:a aac" //Don't use copy when source is AAC, it caused errors in the MSE media element
					}

					// Currently the development machine has ffmpeg version 5.1.2-3, while live has version 4.4.2-0.
					// 5
					//.1.2-3 does not support the -stimeout parameter for rtsp. Until the versions are in line again
					// only use -stimer for live and not dev.
					var stimeout string = "-stimeout 1000000 "
					uri := stream.NetcamUri
					rtspTransport := camera.RtspTransport

					if camera.UseRtspAuth { // Use credentials if required
						idx := len("rtsp://")
						uri = uri[:idx] + url.QueryEscape(creds.CamerasAdminUserName) + ":" + url.QueryEscape(creds.CamerasAdminPassword) + "@" + uri[idx:]
					}

					cmdStr := fmt.Sprintf("/usr/local/bin/ffmpeg -loglevel warning -hide_banner %s-fflags nobuffer -rtsp_transport %s -i  %s -c:v copy %s -async 1 -movflags empty_moov+omit_tfhd_offset+frag_keyframe+default_base_moof -frag_size 10 -f mp4 %s", stimeout, rtspTransport, uri, audio, stream.MediaServerInputUri)
					cmdStr += " 2>&1 >/dev/null | ts '[%Y-%m-%d %H:%M:%S]' >> " + path + "ffmpeg_" + strings.Replace(camera.Name, " ", "_", -1) + "_" + strings.Replace(strings.Replace(stream.Descr, " ", "_", -1), " ", "_", -1) + "_$(date +%Y%m%d).log"
					cmd := exec.Command("bash", "-c", cmdStr)
					stdout, err := cmd.Output()

					if err != nil {
						ee := err.(*exec.ExitError)
						if ee != nil {
							log.Errorf("ffmpeg (%s:%s):- %s, %s", camera.Name, stream.Descr, string(ee.Stderr), ee.Error())
						} else {
							log.Errorf("ffmpeg (%s:%s):- :- %s", camera.Name, stream.Descr, err.Error())
						}
					} else if stdout != nil {
						log.Infof("ffmpeg output (%s:%s):- %s ", camera.Name, stream.Descr, string(stdout))
					}
				}
			}(camera, stream)
		}
	}
}

/*
*
cleanLogs: Clean ffmpeg logs older than 3 weeks
*/
func cleanLogs() {
	path, _ := filepath.Split(config.LogPath)
	for range time.Tick(time.Second * 3600) {
		log.Info("Checking for ffmpeg logs old enough to delete")
		cmd := exec.Command("bash", "-c", "nice -10 find "+path+"ffmpeg_* -mtime +21 -delete")
		_, err := cmd.Output()
		if err != nil {
			ee := err.(*exec.ExitError)
			if ee != nil {
				log.Errorf("Error clearing old logs: %s (%s)", string(ee.Stderr), ee.Error())
			} else {
				log.Errorf("Error clearing old logs: %s", err.Error())
			}
		}
	}
}
