package main

import (
	log "github.com/sirupsen/logrus"
	"os"
	"os/exec"
	"time"
)

type Signal int

const (
	ResetTimer Signal = iota
	StopTimer
)

var signalNames = map[Signal]string{
	ResetTimer: "resetTimer",
	StopTimer:  "stop",
}

type FFMpegTimer struct {
	Timer          *time.Timer
	Cmd            *exec.Cmd
	Signal         chan Signal
	ProcessStopped bool
}

func NewFFmpegTimer(cmd *exec.Cmd) *FFMpegTimer {
	f := &FFMpegTimer{Cmd: cmd, Signal: make(chan Signal, 100), ProcessStopped: false}
	go func() {
		f.Timer = time.NewTimer(6 * time.Second)
		for {
			select {
			case sig := <-f.Signal:
				if sig == ResetTimer {
					f.ResetTimer()
				} else if sig == StopTimer {
					f.Timer.Stop()
					return
				}
			case <-f.Timer.C:
				if f.ProcessStopped {
					return
				}

				err := f.Cmd.Process.Signal(os.Interrupt)
				f.ProcessStopped = true
				if err != nil {
					log.Errorf("Error killing ffmpeg process %d: %s", f.Cmd.Process.Pid, err)
					return
				} else {
					log.Infof("Killed ffmpeg process %d for restart", f.Cmd.Process.Pid)
					return
				}

			}
		}
	}()
	return f
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
	ExecReturn      chan error
}

func NewFeedWatchDog() *FeedWatchDog {
	return &FeedWatchDog{ActiveWatchDogs: map[string]*FFMpegTimer{}, ExecReturn: make(chan error, 1)}
}

// StartActiveWatchDog
// Start active watchdogs on the added suuids
func (f *FeedWatchDog) StartActiveWatchDog(cmd *exec.Cmd, suuids *WatchdogSuuids) {
	for _, suuid := range suuids.Suuids {
		f.ActiveWatchDogs[suuid] = NewFFmpegTimer(cmd)
	}
}

func (f *FeedWatchDog) ClearUpOnExit(processReturnVal error, suuids *WatchdogSuuids) {
	execReturn := processReturnVal
	if execReturn != nil {
		log.Errorf("ffmpeg process returned %s", processReturnVal)
	}
	for _, suuid := range suuids.Suuids {
		awd, ok := f.ActiveWatchDogs[suuid]
		if ok {
			awd.ProcessStopped = true
			awd.Signal <- StopTimer
			log.Infof("Watchdog for suuid %s stopped", suuid)
		}
	}
}

func (f *FeedWatchDog) ResetTimer(suuid string) {
	timer, ok := f.ActiveWatchDogs[suuid]
	if ok {
		timer.Signal <- ResetTimer
	} else {
		log.Errorf("No active watchdog for suuid %s", suuid)
	}
}
