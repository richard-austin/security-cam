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

					// Currently the development machine has ffmpeg version 5.1.2-3, while live has version 4.4.2-0.
					// 5
					//.1.2-3 does not support the -stimeout parameter for rtsp. Until the versions are in line again
					// only use -stimer for live and not dev.
					var stimeout string = "-stimeout 1000000 "
					uri := stream.NetcamUri
					rtspTransport := camera.RtspTransport

					if camera.CameraParamSpecs.CamType == 0 { // General type, credentials used
						idx := len("rtsp://")
						uri = uri[:idx] + url.QueryEscape(creds.CamerasAdminUserName) + ":" + url.QueryEscape(creds.CamerasAdminPassword) + "@" + uri[idx:]
					}

					cmdStr := fmt.Sprintf("/usr/local/bin/ffmpeg -hide_banner -loglevel error %s-fflags nobuffer -rtsp_transport %s -i  %s -c:v copy %s -async 1 -movflags empty_moov+omit_tfhd_offset+frag_keyframe+default_base_moof -frag_size 10 -f mp4 %s", stimeout, rtspTransport, uri, audio, stream.MediaServerInputUri)
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
