package main

import (
	"encoding/json"
	log "github.com/sirupsen/logrus"
	"os"
)

type Config struct {
	LogPath             string  `json:"log_path"`
	LogLevelStr         string  `json:"log_level"`
	CamerasJsonPath     string  `json:"cameras_json_path"`
	ServerPort          int     `json:"server_port"`
	DefaultLatencyLimit float32 `json:"default_latency_limit"`
	GopCache            bool    `json:"gop_cache"`
}

func (c *Config) LogLevel() (err error, level log.Level) {
	levelMap := map[string]log.Level{
		"PANIC": log.PanicLevel,
		"FATAL": log.FatalLevel,
		"ERROR": log.ErrorLevel,
		"WARN":  log.WarnLevel,
		"INFO":  log.InfoLevel,
		"DEBUG": log.DebugLevel,
		"TRACE": log.TraceLevel,
	}

	level, ok := levelMap[c.LogLevelStr]

	if !ok {
		log.Fatalln("Unknown log level specified")
	}

	return
}

type StreamC struct {
	Descr               string `json:"descr"`
	AudioBitRate        string `json:"audio_bitrate"`
	NetcamUri           string `json:"netcam_uri"`
	MediaServerInputUri string `json:"media_server_input_uri"`
	URI                 string `json:"uri"`
}

type Camera struct {
	Name    string             `json:"name"`
	Address string             `json:"address"`
	Streams map[string]StreamC `json:"streams"`
}

type Cameras struct {
	Cameras map[string]Camera `json:"{}"`
}

func (c *Cameras) Suuids() (suuids map[string]string) {
	suuids = map[string]string{}
	for _, camera := range c.Cameras {
		for k, stream := range camera.Streams {
			suuids[camera.Name+" "+stream.Descr] = k
		}
	}
	return
}

func loadConfig() (config *Config, cameras *Cameras) {
	var cams Cameras
	var conf Config

	cameras = &cams
	// Read config.json from the executables directory
	exPath, err := os.Getwd()
	if exPath == "/" {
		// Running as a service
		exPath = "/etc/fmp4-ws-media-server"
	}
	data, err := os.ReadFile(exPath + "/config.json")
	if err != nil {
		log.Fatalln(err)
	}
	err = json.Unmarshal(data, &conf)
	if err != nil {
		log.Fatalln(err)
	}
	data, err = os.ReadFile(conf.CamerasJsonPath)
	if err != nil {
		log.Fatalln(err)
	}
	err = json.Unmarshal(data, &cams.Cameras)
	if err != nil {
		log.Fatalln(err)
	}

	config = &conf
	return
}