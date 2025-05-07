package main

import (
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
	"io"
	"os"
	"os/exec"
	"path/filepath"
)

var cameras *Cameras
var config *Config
var codecs *MimeCodecs
var ffmpegProcs map[string]*exec.Cmd

func main() {
	var customFormatter = log.TextFormatter{}
	customFormatter.TimestampFormat = "2006-01-02 15:04:05"
	customFormatter.FullTimestamp = true
	var formatter log.Formatter = &customFormatter
	log.SetFormatter(formatter)

	config, cameras = loadConfig()
	codecs = NewMimeCodecs()
	ffmpegProcs = make(map[string]*exec.Cmd)

	_, level := config.LogLevel()
	log.SetLevel(level)
	lumberjackLogger := &lumberjack.Logger{
		Filename:   filepath.ToSlash(config.LogPath),
		MaxSize:    5, // MB
		MaxBackups: 10,
		MaxAge:     30, // days
		Compress:   true,
	}
	gin.DefaultWriter = io.MultiWriter(os.Stdout, lumberjackLogger)
	log.SetOutput(io.MultiWriter(os.Stdout, lumberjackLogger))
	ffmpegFeed(config, cameras, &ffmpegProcs)
	serveHTTP()
}
