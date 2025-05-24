package main

import (
	"encoding/json"
	"errors"
	"fmt"
	log "github.com/sirupsen/logrus"
	"os/exec"
	"slices"
	"strconv"
	"strings"
)

/**
  Determine the MIME codec string for the browser VideoDecoder from the device rtsp stream.
*/

type FFProbeStream struct {
	CodecType     string `json:"codec_type"`
	CodecName     string `json:"codec_name"`
	Profile       string `json:"profile"`
	Width         int    `json:"width"`
	Height        int    `json:"height"`
	Level         int    `json:"level"`
	CodecLongName string `json:"codec_long_name"`
}

type FFProbeAudioStream struct {
	CodecType  string `json:"codec_type"`
	CodecName  string `json:"codec_name"`
	SampleFmt  string `json:"sample_fmt"`
	SampleRate string `json:"sample_rate"`
	Channels   int    `json:"channels"`
	BitRate    string `json:"bit_rate"`
}

type FFProbeStreams struct {
	Streams []FFProbeStream `json:"streams"`
}

type FFProbeAudioStreams struct {
	Streams []FFProbeAudioStream `json:"streams"`
}
type AVInfo struct {
	Codec     string
	AudioInfo FFProbeAudioStream
}

func NewAVInfo() (avInfo AVInfo) {
	avInfo = AVInfo{Codec: ""}
	return
}

type MimeCodecs struct {
	AVInfos map[string]*AVInfo // Map codec by suuid
}

func NewMimeCodecs() (mimeCodecs *MimeCodecs) {
	mimeCodecs = &MimeCodecs{AVInfos: map[string]*AVInfo{}}
	return
}

type AudioData struct {
	stream map[string]FFProbeAudioStream
}

func NewAudioData() (audioData *AudioData) {
	audioData = &AudioData{stream: map[string]FFProbeAudioStream{}}
	return
}

/*
		 See https://developer.apple.com/documentation/http-live-streaming/hls-authoring-specification-for-apple-devices-appendixes
	     for codec info.
*/
func (codecs *MimeCodecs) getAVData(rtspUrl string, suuid string) (stream FFProbeStream, audioStream FFProbeAudioStream, err error) {
	log.Info("rtspurl = " + rtspUrl)
	ffprobeCmd := "/usr/bin/ffprobe -hide_banner -timeout 10000000 -i " + rtspUrl + " -threads 5 -v info -print_format json -show_streams -show_chapters -show_format -show_data"
	out, err := exec.Command("bash", "-c", ffprobeCmd).Output()
	result := ""
	if err == nil {
		//str := string(out[:])
		var streams FFProbeStreams
		err := json.Unmarshal(out, &streams)
		if err == nil {
			idx := slices.IndexFunc(streams.Streams, func(stream FFProbeStream) bool { return stream.CodecType == "video" })
			if idx == -1 {
				err = errors.New("No video stream at " + rtspUrl)
			} else {
				stream = streams.Streams[idx]
				result, err = codecs.constructCodecString(stream)
				level := fmt.Sprintf("%X", stream.Level)
				log.Info(stream.CodecName + ": width " + strconv.Itoa(stream.Width) + ": Profile = " + stream.Profile + ": Level = 0x" + level)
			}

			var audioStreams FFProbeAudioStreams
			err = json.Unmarshal(out, &audioStreams)
			idx = slices.IndexFunc(audioStreams.Streams, func(stream FFProbeAudioStream) bool { return stream.CodecType == "audio" })
			if idx == -1 {
				log.Info("No audio stream at " + rtspUrl)
			} else {
				audioStream = audioStreams.Streams[idx]
				log.Info(stream.CodecName + ": Sample rate = " + audioStream.SampleRate + ": Channels = " + strconv.Itoa(audioStream.Channels) + ": Bit rate = " + audioStream.BitRate)
			}
		}
		audioData.stream[suuid] = audioStream
		avi := NewAVInfo()
		avi.Codec = result
		avi.AudioInfo = audioStream
		codecs.AVInfos[suuid] = &avi
	}
	return stream, audioStream, err
}
func (codecs *MimeCodecs) getAVCodecs(suuid string) (avInfo *AVInfo, err error) {
	err = nil
	avInfo, ok := codecs.AVInfos[suuid]
	if !ok {
		err = errors.New("No codec found for suuid " + suuid)
	}
	return
}

func (_ *MimeCodecs) constructCodecString(stream FFProbeStream) (string, error) {
	var err error = nil
	var codec = ""

	switch stream.CodecName {
	case "hevc":
		codec = "hvc1"
		if stream.Profile == "Main" {
			codec += ".1.4"
		} else if stream.Profile == "Main 10" {
			codec += ".2.4"
		} else {
			err = errors.New("Unknown profile " + stream.Profile)
			break
		}

		codec += ".L" + strconv.Itoa(stream.Level)
		codec += ".B0"
		break
	case "h264":
		codec = "avc1"
		if stream.Profile == "Baseline" {
			codec += ".4200"
		} else if stream.Profile == "Main" {
			codec += ".4D40"
		} else if stream.Profile == "High" {
			codec += ".6400"
		} else {
			err = errors.New("Unknown profile " + stream.Profile)
			break
		}
		codec += fmt.Sprintf("%X", stream.Level)
		break
	default:
		err = errors.New("Unknown codec name " + stream.CodecName + "(" + stream.CodecLongName + ")")
	}
	return codec, err
}

func (_ *MimeCodecs) suuidFromUrl(url string) (suuid string, err error) {
	err = nil
	const ref = "?suuid="
	var refIdx = strings.Index(url, ref)
	if refIdx == -1 {
		err = errors.New("Invalid url for obtaining suuid: " + url)
		suuid = ""
	} else {
		var suuidIdx = refIdx + len(ref)
		suuid = url[suuidIdx:]
	}
	return
}
