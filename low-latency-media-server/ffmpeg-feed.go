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

/*
 * getCredentials: Get the camera credentials from the encrypted string cred in the camera data.
 * Private key generated like this: -
 *	openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_keygen_pubexp:65537 | openssl pkcs8 -topk8 -nocrypt -outform der > /etc/security-cam/id_rsa
 *
 * And the public key is created from that with: -
 *	openssl pkey -pubout -inform der -outform der -in /etc/security-cam/id_rsa -out rsa-2048-public-key.spki
 *
 * And the base64 format used in the encryption.ts on the client is created with: -
 * cat rsa-2048-public-key.spki | base64
 */
func getCredentials(cam Camera) (err error, credentials Credentials) {
	data, err := os.ReadFile(config.PrivateKeyPath)
	if err != nil {
		log.Errorf("Error in getCredentials reading private key (%s)", err.Error())
		return
	}
	str := base64.StdEncoding.EncodeToString(data)
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

func ffmpegFeed(config *Config, cameras *Cameras, feedWatchDog *FeedWatchDog) {
	go cleanLogs()
	path, _ := filepath.Split(config.LogPath)
	for _, camera := range cameras.Cameras {
		for _, stream := range camera.Streams {
			go func(camera Camera, stream *StreamC) {
				for {
					//time.Sleep(300 * time.Hour)
					suuid, err := codecs.suuidFromUrl(stream.MediaServerInputUri)
					watchdogSuuids := NewWatchdogSuuids()
					if err != nil {
						log.Error(err.Error())
					}
					audioMode := "-an"
					audio := ""
					tee2 := ""
					if stream.Audio {
						if stream.AudioEncoding != "AAC" {
							audioMode = "-c:a aac -ar 16000 -af asetpts=PTS+0.12/TB"
						} else {
							audioMode = "-c:a aac -ar 16000 -af asetpts=PTS+0.12/TB"
						}
						audio = fmt.Sprintf("[select=a:f=adts:onfail=abort:avioflags=direct:fflags=nobuffer+flush_packets]%sa", stream.MediaServerInputUri)
						tee2 = fmt.Sprintf(" %s -f tee -map 0:a ", audioMode)
						watchdogSuuids.AddSuuid(suuid + "a")
					}

					recording := ""
					if stream.Recording.Enabled && stream.Recording.RecordingInputUrl != "" {
						if stream.Audio {
							tee2 = fmt.Sprintf(" %s -c:v copy -f tee -copytb 1 -map 0:v -map 0:a ", audioMode)
						} else {
							tee2 = " -c:v copy -f tee -copytb 1 -map 0:v "
						}
						recording = fmt.Sprintf("[use_fifo=1:fifo_options=drop_pkts_on_overflow=1:movflags=empty_moov+omit_tfhd_offset+frag_keyframe+default_base_moof:frag_duration=10:f=mp4:onfail=abort]%s", stream.Recording.RecordingInputUrl)
						watchdogSuuids.AddSuuid(suuid + "r")
					}
					if tee2 != "" {
						var sb strings.Builder
						sb.WriteString(tee2)
						if audio != "" {
							sb.WriteString(audio)
						}
						if audio != "" && recording != "" {
							sb.WriteString("|")
						}
						if recording != "" {
							sb.WriteString(recording)
						}

						tee2 = sb.String()
					}
					netcamUri := stream.NetcamUri
					rtspTransport := strings.ToLower(camera.RtspTransport)
					uri := stream.NetcamUri
					if camera.UseRtspAuth { // Use credentials if required
						_, creds := getCredentials(camera)
						idx := len("rtsp://")
						netcamUri = uri[:idx] + url.QueryEscape(creds.UserName) + ":" + url.QueryEscape(creds.Password) + "@" + uri[idx:]
					}
					if err != nil {
						log.Error(err.Error())
					}
					streamInfo, err := codecs.setCodecString(netcamUri, suuid)
					if err != nil {
						log.Error(err.Error())
					}

					codec, err := codecs.getCodecString(suuid)
					log.Info("Codec string = " + codec)
					var sb strings.Builder
					//	sb.WriteString(fmt.Sprintf("ffmpeg -f v4l2 -i /dev/video0 -f pulse -i default -ac 2 -c:v libx264 -c:a aac -preset ultrafast -tune zerolatency -f tee -map 0:v %s \"[select=v:f=h264:onfail=abort]%s %s %s\"", "-map 1:a", stream.MediaServerInputUri, audio, recording))
					sb.WriteString(fmt.Sprintf("-loglevel %s -hide_banner -timeout 3000000 -rtsp_transport %s -i %s -c:v copy -an -copytb 1 -f tee -fflags nobuffer -map 0:v [select=v:f=%s:onfail=abort:avioflags=direct:fflags=nobuffer+flush_packets]%s%s", config.FfmpegLogLevelStr, rtspTransport, netcamUri, streamInfo.CodecName, stream.MediaServerInputUri, tee2))
					log.Infof("/usr/bin/ffmpeg %s", sb.String())
					cmdStr := sb.String()
					cmd := exec.Command("/usr/bin/ffmpeg", strings.Split(cmdStr, " ")...)
					watchdogSuuids.AddSuuid(suuid)
					log.Infof("Starting ffmpeg feed for %s %s", camera.Name, stream.Descr)
					feedWatchDog.StartActiveWatchDog(cmd, watchdogSuuids)
					file, err := os.OpenFile(path+"ffmpeg_"+camera.Name+"_"+stream.Descr+"_"+time.Now().Format("20060102")+".log", os.O_CREATE|os.O_APPEND|os.O_RDWR, 0644)
					if err != nil {
						log.Errorf("Error creating ffmpeg log file: %s", err.Error())
					}
					cmd.Stderr = file // Normal output on ffmpeg comes out on stderr
					feedWatchDog.ClearUpOnExit(cmd.Run(), watchdogSuuids)
					err = file.Close()
					if err != nil {
						log.Errorf("Error closing ffmpeg log file: %s", err.Error())
					}
					time.Sleep(10 * time.Second)
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
