package main

import (
	"fmt"
	log "github.com/sirupsen/logrus"
	"net/url"
	"os/exec"
	"time"
)

func ffmpegFeed(cameras *Cameras, creds *CameraCredentials) {
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
						audio = "-c:a copy"
					}
					uri := stream.NetcamUri
					rtsp_transport := "tcp"

					if camera.CameraParamSpecs.CamType == 0 { // General type, credentials used
						idx := len("rtsp://")
						uri = uri[:idx] + url.QueryEscape(creds.CamerasAdminUserName) + ":" + url.QueryEscape(creds.CamerasAdminPassword) + "@" + uri[idx:]
					}

					cmdStr := fmt.Sprintf("/usr/bin/ffmpeg -hide_banner -loglevel error -stimeout 1000000 -fflags nobuffer -rtsp_transport %s -i  %s -c:v copy %s -async 1 -movflags empty_moov+omit_tfhd_offset+frag_keyframe+default_base_moof -frag_size 10 -preset superfast -tune zerolatency -f mp4 %s", rtsp_transport, uri, audio, stream.MediaServerInputUri)
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
