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

type FFProbeStreams struct {
	Streams []FFProbeStream `json:"streams"`
}

type MimeCodecs struct {
	Codecs map[string]string // Map codec by suuid
}

func NewMimeCodecs() (mimeCodecs *MimeCodecs) {
	mimeCodecs = &MimeCodecs{Codecs: map[string]string{}}
	return
}

/*
		 See https://developer.apple.com/documentation/http-live-streaming/hls-authoring-specification-for-apple-devices-appendixes
	     for codec info.
*/
func (codecs *MimeCodecs) setCodecString(rtspUrl string, suuid string) (stream FFProbeStream, err error) {
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
		}
		codecs.Codecs[suuid] = result
	}
	return stream, err
}
func (codecs *MimeCodecs) getCodecString(suuid string) (mimeCodec string, err error) {
	err = nil
	mimeCodec, ok := codecs.Codecs[suuid]
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
