package main

import (
	"encoding/json"
	log "github.com/sirupsen/logrus"
	"os"
)

type Config struct {
	LogPath             string  `json:"log_path"`
	LogLevelStr         string  `json:"log_level"`
	FfmpegLogLevelStr   string  `json:"ffmpeg_log_level"`
	CamerasJsonPath     string  `json:"cameras_json_path"`
	PrivateKeyPath      string  `json:"private_key_path"`
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
		log.Fatalln("Unknown log level specified{", c.LogLevelStr, ")")
		panic(err)
	}

	return
}

func (c *Config) FfmpegLogLevel() (err error, level string) {
	levelMap := map[string]string{
		"quiet":   "quiet",
		"panic":   "panic",
		"fatal":   "fatal",
		"error":   "error",
		"warning": "warning",
		"info":    "info",
		"verbose": "verbose",
		"debug":   "debug",
		"trace":   "trace",
		"-8":      "quiet",
		"0":       "panic",
		"8":       "fatal",
		"16":      "error",
		"24":      "warning",
		"32":      "info",
		"40":      "verbose",
		"48":      "debug",
		"56":      "trace",
	}
	level, ok := levelMap[c.FfmpegLogLevelStr]
	if !ok {
		log.Fatalln("Unknown ffmpeg log level specified (", c.FfmpegLogLevelStr, ")")
	}
	return
}

type StreamC struct {
	Descr               string `json:"descr"`
	Audio               bool   `json:"audio"`
	AudioEncoding       string `json:"audio_encoding"`
	NetcamUri           string `json:"netcam_uri"`
	MediaServerInputUri string `json:"media_server_input_uri"`
	URI                 string `json:"uri"`
	PreambleFrames      int    `json:"preambleFrames"`
}

type CameraParamSpecs struct {
	CamType int    `json:"camType"`
	Params  string `json:"params"`
	Uri     string `json:"uri"`
	Name    string `json:"name"`
}

type Camera struct {
	Name                      string             `json:"name"`
	Address                   string             `json:"address"`
	Streams                   map[string]StreamC `json:"streams"`
	CamType                   int                `json:"camType"`
	CameraParamSpecs          CameraParamSpecs   `json:"cameraParamSpecs"`
	BackChannelAudioSupported bool               `json:"backChannelAudioSupported"`
	RtspTransport             string             `json:"rtspTransport"`
	UseRtspAuth               bool               `json:"useRtspAuth"`
	Cred                      string             `json:"cred"`
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
		log.Errorln(err)
	}
	err = json.Unmarshal(data, &conf)
	if err != nil {
		log.Errorln(err)
	}
	data, err = os.ReadFile(conf.CamerasJsonPath)
	if err != nil {
		log.Errorln(err)
	}
	err = json.Unmarshal(data, &cams.Cameras)
	if err != nil {
		log.Errorln(err)
	}

	config = &conf
	return
}
