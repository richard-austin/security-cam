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
	const cacheLength int = 90
	cache = GopCache{Cache: make([]Packet, cacheLength), inputIndex: 0, cacheLength: cacheLength - 1, GopCacheUsed: used}
	return
}

func (g *GopCache) Input(p Packet) (err error) {
	err = nil
	if !g.GopCacheUsed {
		return
	}
	g.mutex.Lock()
	defer g.mutex.Unlock()
	if (p.isMoof() || g.inputIndex == 0) && p.isKeyFrame() {
		g.inputIndex = 0
	}
	if g.inputIndex < g.cacheLength {
		g.Cache[g.inputIndex] = p
		g.inputIndex++
	} else {
		err = fmt.Errorf("GOP Cache is full")
	}
	return
}

func (g *GopCache) GetSnapshot() (snapshot *GopCacheSnapshot) {
	if !g.GopCacheUsed {
		return
	}
	snapshot = newFeeder(g)
	return
}
func newFeeder(g *GopCache) (feeder *GopCacheSnapshot) {
	feeder = &GopCacheSnapshot{pktChan: make(chan Packet, g.cacheLength)}
	g.mutex.Lock()
	defer g.mutex.Unlock()
	for _, pkt := range g.Cache[:g.inputIndex] {
		feeder.pktChan <- pkt
	}
	return
}

func (s GopCacheSnapshot) Get(live chan Packet) (packet Packet) {
	select {
	case packet = <-s.pktChan:
	default:
		packet = <-live
	}
	return
}
