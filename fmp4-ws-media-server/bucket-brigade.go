package main

import (
	"sync"
)

type BucketBrigadeFeeder struct {
	mutex        sync.Mutex
	lastBBIdx    int
	gopCacheCopy *GopCacheCopy
	pktFeed      chan Packet
}

type BucketBrigade struct {
	bbUsed      bool
	cacheLength int
	mutex       sync.Mutex
	inputIndex  int
	Cache       []Packet
	started     bool
	gopCache    GopCache
}

func NewBucketBrigade(used bool) (bucketBrigade BucketBrigade) {
	const cacheLength int = 400
	bucketBrigade = BucketBrigade{
		Cache:       make([]Packet, cacheLength),
		started:     false,
		inputIndex:  0,
		cacheLength: cacheLength,
		bbUsed:      used,
		gopCache:    NewGopCache(true)}
	return
}

func newBucketBrigadeFeeder(bucketBrigade *BucketBrigade) (bbFeeder *BucketBrigadeFeeder) {
	bbFeeder = &BucketBrigadeFeeder{
		lastBBIdx:    0,
		gopCacheCopy: bucketBrigade.gopCache.GetCurrent(),
		pktFeed:      make(chan Packet, 1),
	}

	callbacks.bbf = append(callbacks.bbf, bbFeeder)
	return
}

func (bb *BucketBrigade) Input(p Packet) (err error) {
	err = nil
	if !bb.bbUsed {
		return
	}
	bb.mutex.Lock()
	defer bb.mutex.Unlock()

	//	fmt.Printf("Input index = %d Packet length = %d\n", bb.inputIndex, len(p.pckt))
	bb.Cache[bb.inputIndex] = p
	if bb.started {
		opIdx := (bb.inputIndex + 1) % bb.cacheLength
		err = bb.gopCache.Input(bb.Cache[opIdx])
		for _, cb := range callbacks.bbf {
			cb.pktFeed <- bb.Cache[opIdx]
		}
	}
	if bb.inputIndex < bb.cacheLength-1 {
		bb.inputIndex++
	} else {
		bb.inputIndex = 0
		bb.started = true
	}
	return
}

func (bb *BucketBrigade) GetCurrent() (bbFeeder *BucketBrigadeFeeder) {
	if !bb.bbUsed {
		return
	}
	bb.mutex.Lock()
	defer bb.mutex.Unlock()
	bbFeeder = newBucketBrigadeFeeder(bb)
	return
}

func (bb *BucketBrigadeFeeder) callback(pkt Packet) {
	bb.pktFeed <- pkt
	//	fmt.Printf("callback packet length = %d\n", len(pkt.pckt))
}

func (bb *BucketBrigadeFeeder) destroy() {
	bb.mutex.Lock()
	defer bb.mutex.Unlock()
	for i := 0; i < len(callbacks.bbf); i++ {
		if bb == callbacks.bbf[i] {
			callbacks.bbf = append(callbacks.bbf[:i], callbacks.bbf[i+1:]...)
			break
		}
	}
}

func (bb *BucketBrigadeFeeder) Get() (packet Packet) {
	packet = bb.gopCacheCopy.Get(bb.pktFeed)
	return
}
