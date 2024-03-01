package main

import (
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"fmt"
	log "github.com/sirupsen/logrus"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

type Credentials struct {
	UserName string `json:"userName"`
	Password string `json:"password"`
}

func getCredentials(cam Camera) (err error, credentials Credentials) {
	bytes, err := os.ReadFile("/home/richard/cloud-server/xtrn-files-and-config/privateKey")
	if err != nil {
		log.Errorf("Error in getCredentials reading private key (%s)", err.Error())
		return
	}
	str := base64.StdEncoding.EncodeToString(bytes)
	privateKeyBlock, _ := pem.Decode([]byte("-----BEGIN PRIVATE KEY-----\n" + str + "\n-----END PRIVATE KEY-----"))
	get, err := x509.ParsePKCS8PrivateKey(privateKeyBlock.Bytes)
	if err != nil {
		log.Errorf("Error in getCredentials parsing private key (%s)", err.Error())
		return
	}
	pk := get.(*rsa.PrivateKey) // Cast to type that rsa.DecryptOAEP can use
	encrypted, _ := base64.StdEncoding.DecodeString(cam.Cred)
	if len(encrypted) > 0 {
		decryptedData, err := rsa.DecryptOAEP(sha256.New(), nil, pk, encrypted, nil)
		if err != nil {
			log.Errorf("Decrypt data error in getCredentials: %s", err.Error())
			panic(err)
		}
		err = json.Unmarshal(decryptedData, &credentials)
		if err != nil {
			log.Errorf("Error unmarshalling JSON in getCredentials: %s", err.Error())
			panic(err)
		}
	}
	return
}
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

					var stimeout string = "-stimeout 1000000 "
					uri := stream.NetcamUri
					rtspTransport := camera.RtspTransport

					if camera.UseRtspAuth { // Use credentials if required
						_, creds := getCredentials(camera)
						idx := len("rtsp://")
						uri = uri[:idx] + url.QueryEscape(creds.UserName) + ":" + url.QueryEscape(creds.Password) + "@" + uri[idx:]
					}
					// Using ffmpeg version 4.4.4 (built on a Raspberry pi and deployed by the .deb file) as versions 5+ don't
					//  work with RTSP streams with no time stamps when producing fragmented mp4 with audio. All my cameras
					//  omit to set time stamps so this is my solution for now.
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
