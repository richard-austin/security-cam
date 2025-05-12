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

func ffmpegFeed(config *Config, cameras *Cameras, ffmpegProcs *map[string]*exec.Cmd) {
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
					if !stream.Audio {
						audioMode = "-an"
						audio = ""
						audioMap = ""
					} else {
						if stream.AudioEncoding != "AAC" {
							audioMode = "-c:a aac -ar 12000 -af asetpts=PTS+0.4/TB"
						} else {
							audioMode = "-c:a aac -ar 12000 -af asetpts=PTS+0.4/TB"
						}
						audio = fmt.Sprintf("|[use_fifo=1:fifo_options=drop_pkts_on_overflow=1:select=a:fflags=igndts:f=adts:onfail=abort]%sa", stream.MediaServerInputUri)
						audioMap = "-map 0:a"
					}

					recording := ""
					if stream.Recording.Enabled && stream.Recording.RecordingInputUrl != "" {
						recording = fmt.Sprintf("|[use_fifo=1:fifo_options=drop_pkts_on_overflow=1:movflags=empty_moov+omit_tfhd_offset+frag_keyframe+default_base_moof:frag_duration=10:f=mp4:onfail=abort]%s", stream.Recording.RecordingInputUrl)
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
					streamInfo, err := codecs.setCodecString(netcamUri, suuid)
					if err != nil {
						log.Error(err.Error())
					}

					codec, err := codecs.getCodecString(suuid)
					log.Info("Codec string = " + codec)
					var sb strings.Builder
					//	sb.WriteString(fmt.Sprintf("ffmpeg -f v4l2 -i /dev/video0 -f pulse -i default -ac 2 -c:v libx264 -c:a aac -preset ultrafast -tune zerolatency -f tee -map 0:v %s \"[select=v:f=h264:onfail=abort]%s %s %s\"", "-map 1:a", stream.MediaServerInputUri, audio, recording))
					sb.WriteString(fmt.Sprintf("/usr/bin/ffmpeg -loglevel %s -hide_banner -timeout 3000000 -fflags nobuffer -rtsp_transport %s -i %s -c:v copy %s -f tee -fflags nobuffer -map 0:v %s \"[use_fifo=1:fifo_options=drop_pkts_on_overflow=1:select=v:f=%s:onfail=abort]%s%s%s\"", config.FfmpegLogLevelStr, rtspTransport, netcamUri, audioMode, audioMap, streamInfo.CodecName, stream.MediaServerInputUri, audio, recording))
					log.Info(sb.String())
					if config.FfmpegLogLevelStr != "quiet" {
						sb.WriteString(" 2>&1 >/dev/null | ts '[%Y-%m-%d %H:%M:%S]' >> " + path + "ffmpeg_" + strings.Replace(camera.Name, " ", "_", -1) + "_" + strings.Replace(strings.Replace(stream.Descr, " ", "_", -1), " ", "_", -1) + "_$(date +%Y%m%d).log")
					}
					cmdStr := sb.String()
					cmd := exec.Command("bash", "-c", cmdStr)
					(*ffmpegProcs)[suuid] = cmd
					var out bytes.Buffer
					var stderr bytes.Buffer
					cmd.Stdout = &out
					cmd.Stderr = &stderr
					err = cmd.Run()
					if err != nil {
						log.Errorf("%s:%s -- %s: %s", camera.Name, stream.Descr, fmt.Sprint(err), stderr.String())
					} else if cmd.Stdout != nil {
						log.Info(out.String())
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
