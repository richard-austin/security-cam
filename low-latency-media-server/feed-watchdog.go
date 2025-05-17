package main

import (
	log "github.com/sirupsen/logrus"
	"os"
	"os/exec"
	"time"
)

type FFMpegTimer struct {
	Timer *time.Timer
	Cmd   *exec.Cmd
}

func NewFFmpegTimer(cmd *exec.Cmd) *FFMpegTimer {
	t := &FFMpegTimer{Cmd: cmd}
	return t
}

func (f *FFMpegTimer) StartTimer() {
	f.Timer = time.NewTimer(3 * time.Second)
	go func() {
		<-f.Timer.C
		err := f.Cmd.Process.Signal(os.Interrupt)
		if err != nil {
			log.Errorf("Error killing ffmpeg process %d: %s", f.Cmd.Process.Pid, err)
			return
		} else {
			log.Warnf("Killing ffmpeg process %d", f.Cmd.Process.Pid)
		}
	}()
}

func (f *FFMpegTimer) ResetTimer() {
	result := f.Timer.Reset(5 * time.Second)
	if !result {
		log.Errorf("Error resetting timer")
	}
}

type WatchdogSuuids struct {
	Suuids []string
}

func NewWatchdogSuuids() *WatchdogSuuids {
	return &WatchdogSuuids{Suuids: []string{}}
}

func (w *WatchdogSuuids) AddSuuid(suuid string) {
	w.Suuids = append(w.Suuids, suuid)
}

type FeedWatchDog struct {
	ActiveWatchDogs map[string]*FFMpegTimer
	NewSuuids       []string
}

func NewFeedWatchDog() *FeedWatchDog {
	return &FeedWatchDog{ActiveWatchDogs: map[string]*FFMpegTimer{}, NewSuuids: []string{}}
}

// AddSuuids
// Add new suuids to the watchdog for a single ffmpeg feed
func (f *FeedWatchDog) AddSuuids(suuids *WatchdogSuuids) {
	for _, suuid := range suuids.Suuids {
		f.NewSuuids = append(f.NewSuuids, suuid)
	}
}

// StartActiveWatchDog
// Start active watchdogs on the added suuids
func (f *FeedWatchDog) StartActiveWatchDog(cmd *exec.Cmd) {
	for _, suuid := range f.NewSuuids {
		f.ActiveWatchDogs[suuid] = NewFFmpegTimer(cmd)
	}
	f.NewSuuids = []string{}
}
func (f *FeedWatchDog) StartTimer(suuid string) {
	awd, ok := f.ActiveWatchDogs[suuid]
	if ok {
		awd.StartTimer()
	} else {
		log.Errorf("No active watchdog for suuid %s", suuid)
	}
}

func (f *FeedWatchDog) ResetTimer(suuid string) {
	timer, ok := f.ActiveWatchDogs[suuid]
	if ok {
		timer.ResetTimer()
	} else {
		log.Errorf("No active watchdog for suuid %s", suuid)
	}
}
