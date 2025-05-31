package main

import (
	"fmt"
	"sync"
)

type GopCache struct {
	GopCacheUsed bool
	cacheLength  int
	mutex        sync.Mutex
	inputIndex   int
	Cache        []Packet
}

type GopCacheSnapshot struct {
	pktChan chan Packet
}

func NewGopCache(used bool) (cache GopCache) {
	const cacheLength int = 2048 // Need LARGE GOP cache for the flv recording stream. The key frame intervals are
	// larger than with the plain video stream, and the flv stream can also have audio packets
	cache = GopCache{Cache: make([]Packet, cacheLength), cacheLength: cacheLength - 1, inputIndex: 0, GopCacheUsed: used}
	return
}

// Input
// Input packets to the GOP cache starting with the latest keyframe at index 0
func (g *GopCache) Input(p Packet) (err error) {
	err = nil
	if !g.GopCacheUsed {
		return
	}
	g.mutex.Lock()
	defer g.mutex.Unlock()

	if p.isKeyFrame() {
		g.inputIndex = 0
	}
	if g.inputIndex < g.cacheLength {
		g.Cache[g.inputIndex] = p
		//	log.Error("Video gop cache size = " + strconv.Itoa(g.inputIndex))
		g.inputIndex++
	} else {
		err = fmt.Errorf("GOP Cache is full")
	}
	return
}

func (g *GopCache) RecordingInput(p Packet) (err error) {
	err = nil
	if !g.GopCacheUsed {
		return
	}
	g.mutex.Lock()
	defer g.mutex.Unlock()

	if p.isFlvKeyFrame() {
		g.inputIndex = 0
	}
	if g.inputIndex < g.cacheLength {
		g.Cache[g.inputIndex] = p
		g.inputIndex++
	} else {
		err = fmt.Errorf("GOP Cache is full")
	}

	//log.Infof("fmp4 gop cache index = %d", g.inputIndex)
	return
}

// GetSnapshot
// Create a new GOP cache snapshot from the current GOP cache, unless GOP cache is not enabled
// **
func (g *GopCache) GetSnapshot() (snapshot *GopCacheSnapshot) {
	if !g.GopCacheUsed {
		return
	}
	snapshot = g.newFeeder()
	return
}

// newFeeder
// Create a new GOP cache snapshot from the current GOP cache
// **
func (g *GopCache) newFeeder() (feeder *GopCacheSnapshot) {
	g.mutex.Lock()
	defer g.mutex.Unlock()
	feeder = &GopCacheSnapshot{pktChan: make(chan Packet, g.inputIndex)}

	for _, pkt := range g.Cache[:g.inputIndex] {
		feeder.pktChan <- pkt
	}
	return
}

// Get
// Get: Get the live feed, prioritising the GOP cache snapshot content before sending live feed to the client
// **
func (s GopCacheSnapshot) Get(live chan Packet) (packet Packet) {
	select {
	case packet = <-s.pktChan:
	default:
		packet = <-live
	}
	return
}
