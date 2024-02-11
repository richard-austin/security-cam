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

type GopCacheFeeder struct {
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

func (g *GopCache) GetFeeder() (gopCacheCopy *GopCacheFeeder) {
	if !g.GopCacheUsed {
		return
	}
	g.mutex.Lock()
	defer g.mutex.Unlock()
	gopCacheCopy = newGopCacheCopy(g)
	return
}
func newGopCacheCopy(cache *GopCache) (feeder *GopCacheFeeder) {
	feeder = &GopCacheFeeder{pktChan: make(chan Packet, cache.cacheLength)}
	for _, pkt := range cache.Cache[:cache.inputIndex] {
		feeder.pktChan <- pkt
	}
	return
}

func (c GopCacheFeeder) Get(live chan Packet) (packet Packet) {
	select {
	case packet = <-c.pktChan:
	default:
		packet = <-live
	}
	return
}
