package main

import (
	"sync"
)

// BucketBrigade /** A cache used to delay the video so that recordings triggered by camara's FTP upload have some preamble
//
//	/** to the point at which action was detected
type BucketBrigade struct {
	bbUsed      bool
	cacheLength int
	mutex       sync.Mutex
	inputIndex  int
	Cache       []Packet
	started     bool
	gopCache    GopCache
}

// BucketBrigadeFeeder // Combines a (delayed input) GopCacheCopy with the delayed feed from a bucket brigade instance
type BucketBrigadeFeeder struct {
	mutex        sync.Mutex
	lastBBIdx    int
	gopCacheCopy *GopCacheCopy
	pktFeed      chan Packet
}

func NewBucketBrigade(used bool) (bucketBrigade BucketBrigade) {
	const cacheLength int = 200
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

	bbfMarshallPoint.bbf = append(bbfMarshallPoint.bbf, bbFeeder)
	return
}

func (bb *BucketBrigade) Input(p Packet) (err error) {
	err = nil
	if !bb.bbUsed {
		return
	}
	bb.mutex.Lock()
	defer bb.mutex.Unlock()

	bb.Cache[bb.inputIndex] = p
	if bb.started {
		opIdx := (bb.inputIndex + 1) % bb.cacheLength
		err = bb.gopCache.Input(bb.Cache[opIdx])
		for _, cb := range bbfMarshallPoint.bbf {
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

func (bb *BucketBrigadeFeeder) destroy() {
	bb.mutex.Lock()
	defer bb.mutex.Unlock()
	for i := 0; i < len(bbfMarshallPoint.bbf); i++ {
		if bb == bbfMarshallPoint.bbf[i] {
			bbfMarshallPoint.bbf = append(bbfMarshallPoint.bbf[:i], bbfMarshallPoint.bbf[i+1:]...)
			break
		}
	}
}

func (bb *BucketBrigadeFeeder) Get() (packet Packet) {
	packet = bb.gopCacheCopy.Get(bb.pktFeed)
	return
}
