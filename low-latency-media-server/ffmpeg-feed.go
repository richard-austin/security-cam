package main

import (
	"bytes"
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

func ffmpegFeed(config *Config, cameras *Cameras) {
	go cleanLogs()
	path, _ := filepath.Split(config.LogPath)
	for _, camera := range cameras.Cameras {
		for _, stream := range camera.Streams {
			go func(camera Camera, stream *StreamC) {
				for {
					//time.Sleep(300 * time.Hour)
					var audioMode string
					var audio string
					var audioMap string
					var audioMuxer string
					if !stream.Audio {
						audioMode = "-an"
						audio = ""
						audioMap = ""
						audioMuxer = ""
					} else {
						if stream.AudioEncoding != "AAC" {
							audioMuxer = "alaw"
						} else {
							audioMuxer = "adts"
						}
						audioMode = "-c:a copy"
						audio = fmt.Sprintf("|[select=a:f=%s:onfail=abort]%sa", audioMuxer, stream.MediaServerInputUri)
						audioMap = "-map 0:a " // The space at the end is important in splitting the command line
					}

					recording := ""
					if stream.Recording.Enabled && stream.Recording.RecordingInputUrl != "" {
						recording = fmt.Sprintf("|[use_fifo=1:fifo_options=drop_pkts_on_overflow=1:f=flv:onfail=abort]%s", stream.Recording.RecordingInputUrl)
					}
					netcamUri := stream.NetcamUri
					rtspTransport := strings.ToLower(camera.RtspTransport)
					uri := stream.NetcamUri
					if camera.UseRtspAuth { // Use credentials if required
						_, creds := getCredentials(camera)
						idx := len("rtsp://")
						netcamUri = uri[:idx] + url.QueryEscape(creds.UserName) + ":" + url.QueryEscape(creds.Password) + "@" + uri[idx:]
					}
					suuid, err := codecs.suuidFromUrl(stream.MediaServerInputUri)
					if err != nil {
						log.Error(err.Error())
					}
					streamInfo, audioStreamInfo, err := codecs.getAVData(netcamUri, suuid)
					if err != nil {
						log.Error(err.Error())
					} else {
						log.Info("Audio stream info = " + audioStreamInfo.CodecName)
					}
					avi, err := codecs.getAVCodecs(suuid)
					if err != nil {
						log.Errorf("error getting codec for %s: %s", suuid, err.Error())
						time.Sleep(3 * time.Second)
						continue
					}
					log.Info("Codec string = " + avi.Codec)
					var sb strings.Builder
					//	sb.WriteString(fmt.Sprintf("ffmpeg -f v4l2 -i /dev/video0 -f pulse -i default -ac 2 -c:v libx264 -c:a aac -preset ultrafast -tune zerolatency -f tee -map 0:v %s \"[select=v:f=h264:onfail=abort]%s %s %s\"", "-map 1:a", stream.MediaServerInputUri, audio, recording))
					sb.WriteString(fmt.Sprintf("-loglevel %s -hide_banner -timeout 3000000 -rtsp_transport %s -i %s -c:v copy %s -copytb 1 -f tee -fflags nobuffer -map 0:v %s[select=v:f=%s:onfail=abort:avioflags=direct:fflags=nobuffer+flush_packets]%s%s%s", config.FfmpegLogLevelStr, rtspTransport, netcamUri, audioMode, audioMap, streamInfo.CodecName, stream.MediaServerInputUri, audio, recording))
					log.Info("/usr/bin/ffmpeg " + sb.String())
					cmdStr := sb.String()
					cmd := exec.Command("/usr/bin/ffmpeg", strings.Split(cmdStr, " ")...)
					var out bytes.Buffer
					var stderr bytes.Buffer
					cmd.Stdout = &out
					var file *os.File
					if config.FfmpegLogLevelStr != "quiet" {
						file, err = os.OpenFile(path+"ffmpeg_"+strings.Replace(camera.Name+"_"+stream.Descr, " ", "_", -1)+"_"+time.Now().Format("20060102")+".log", os.O_CREATE|os.O_APPEND|os.O_RDWR, 0644)
						if err != nil {
							log.Errorf("Error creating ffmpeg log file: %s", err.Error())
						}
						cmd.Stderr = file // Normal output on ffmpeg comes out on stderr
					} else {
						cmd.Stderr = &stderr
					}
					err = cmd.Run()
					if err != nil {
						log.Errorf("%s:%s -- %s: %s", camera.Name, stream.Descr, fmt.Sprint(err), stderr.String())
					} else if cmd.Stdout != nil {
						log.Info(out.String())
					}

					if config.FfmpegLogLevelStr != "quiet" {
						err = file.Close()
						if err != nil {
							log.Errorf("Error closing ffmpeg log file: %s", err.Error())
						}
					}

					time.Sleep(3 * time.Second)
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
