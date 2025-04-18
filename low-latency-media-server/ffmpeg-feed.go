package main

import (
	"errors"
	"fmt"
	log "github.com/sirupsen/logrus"
	"net/url"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

func ffmpegFeed(config *Config, cameras *Cameras) {
	go cleanLogs()
	path, _ := filepath.Split(config.LogPath)
	for _, camera := range cameras.Cameras {
		for _, stream := range camera.Streams {
			go func(camera Camera, stream *StreamC) {
				for {
					time.Sleep(time.Second)
					var audio string
					if !stream.Audio {
						audio = "-an"
					} else if strings.ToLower(stream.AudioEncoding) != "aac" {
						audio = "-c:a aac"
					} else {
						audio = "-c:a copy"
					}

					var timeout = "-timeout 1000000 "
					netcamUri := stream.NetcamUri
					rtspTransport := strings.ToLower(camera.RtspTransport)

					if camera.Username != "" { // If credentials are given, add them to the URL
						idx := len("rtsp://")
						netcamUri = netcamUri[:idx] + url.QueryEscape(camera.Username) + ":" + url.QueryEscape(camera.Password) + "@" + netcamUri[idx:]
					}
					//	logging :=  "2>&1 >/dev/null | ts '[%Y-%m-%d %H:%M:%S]' >> ${log_dir}ffmpeg_${cam.name.replace(' ', '_') + "_" + stream.descr.replace(' ', '_').replace('.', '_')}_\$(date +%Y%m%d).log"
					suuid, err := codecs.suuidFromUrl(stream.MediaServerInputUri)
					if err != nil {
						log.Error(err.Error())
					}
					streamInfo, err := codecs.setCodecString(netcamUri, suuid)
					if err != nil {
						log.Error(err.Error())
					}

					codec, err := codecs.getCodecString(suuid)
					log.Info("Codec string = " + codec)
					log.Info("Recording src url = " + stream.RecordingSrcURL)
					cmdStr := fmt.Sprintf("/usr/bin/ffmpeg -loglevel warning -hide_banner %s-fflags nobuffer -rtsp_transport %s -i  %s  -c:v copy %s  -f %s -preset ultrafast -tune zero_latency %s -vn -c:a pcm_alaw -b:a 64K -ar 48000 -af atempo=1.03 -f alaw -preset ultrafast -tune zero_latency %sa -c:v copy -c:a aac -async 1 -movflags empty_moov+omit_tfhd_offset+frag_keyframe+default_base_moof -frag_duration 10 -f mp4 %s", timeout, rtspTransport, netcamUri, audio, streamInfo.CodecName, stream.MediaServerInputUri, stream.MediaServerInputUri, stream.RecordingSrcURL)
					log.Info(cmdStr)
					cmdStr += " 2>&1 >/dev/null | ts '[%Y-%m-%d %H:%M:%S]' >> " + path + "ffmpeg_" + strings.Replace(camera.Name, " ", "_", -1) + "_" + strings.Replace(strings.Replace(stream.Descr, " ", "_", -1), " ", "_", -1) + "_$(date +%Y%m%d).log"
					cmd := exec.Command("bash", "-c", cmdStr)
					stdout, err := cmd.Output()
					log.Info(cmdStr)
					if err != nil {
						var ee *exec.ExitError
						errors.As(err, &ee)
						if ee != nil {
							log.Errorf("ffmpeg (%s:%s):- %s, %s", camera.Name, stream.Descr, string(ee.Stderr), ee.Error())
						} else {
							log.Errorf("ffmpeg (%s:%s):- :- %s", camera.Name, stream.Descr, err.Error())
						}
					} else if stdout != nil {
						log.Infof("ffmpeg output (%s:%s):- %s ", camera.Name, stream.Descr, string(stdout))
					}
				}
			}(camera, &stream)
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
