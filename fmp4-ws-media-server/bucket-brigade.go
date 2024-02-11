package main

import (
	log "github.com/sirupsen/logrus"
	"sync"
)

// BucketBrigade /** A cache used to delay the video so that recordings triggered by camara's FTP upload have some preamble
//
//	/** to the point at which action was detected
type BucketBrigade struct {
	bbUsed      bool
	feeders     []*BucketBrigadeFeeder
	cacheLength int
	mutex       sync.Mutex
	inputIndex  int
	Cache       []Packet
	started     bool
	gopCache    GopCache
}

// BucketBrigadeFeeder // Combines a (delayed input) GopCacheSnapshot with the delayed feed from a bucket brigade instance
type BucketBrigadeFeeder struct {
	lastBBIdx        int
	gopCacheSnapshot *GopCacheSnapshot
	pktFeed          chan Packet
}

func NewBucketBrigade(used bool) (bucketBrigade BucketBrigade) {
	const cacheLength int = 200
	bucketBrigade = BucketBrigade{
		Cache:       make([]Packet, cacheLength),
		feeders:     make([]*BucketBrigadeFeeder, 0),
		started:     false,
		inputIndex:  0,
		cacheLength: cacheLength,
		bbUsed:      used,
		gopCache:    NewGopCache(true)}
	return
}

func (bb *BucketBrigade) newFeeder() (bbFeeder *BucketBrigadeFeeder) {
	bbFeeder = &BucketBrigadeFeeder{
		lastBBIdx:        0,
		gopCacheSnapshot: bb.gopCache.GetSnapshot(),
		pktFeed:          make(chan Packet, 300),
	}
	bb.mutex.Lock()
	defer bb.mutex.Unlock()
	bb.feeders = append(bb.feeders, bbFeeder)
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
		for _, f := range bb.feeders {
			select {
			case f.pktFeed <- bb.Cache[opIdx]:
			default:
				log.Errorf("Missed packet in bucket brigade Input: %d items in pktFeed channel\n", len(f.pktFeed))
			}
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

func (bb *BucketBrigade) GetFeeder() (bbFeeder *BucketBrigadeFeeder) {
	if !bb.bbUsed {
		return
	}
	bbFeeder = bb.newFeeder()
	return
}

func (bb *BucketBrigade) DestroyFeeder(bbf *BucketBrigadeFeeder) {
	bb.mutex.Lock()
	defer bb.mutex.Unlock()
	i := 0
	for ; i < len(bb.feeders); i++ {
		if bbf == bb.feeders[i] {
			break
		}
	}
	// Ensure the channel is emptied to prevent Input from blocking
	for len(bb.feeders[i].pktFeed) > 0 {
		_ = <-bb.feeders[i].pktFeed
	}
	bb.feeders = append(bb.feeders[:i], bb.feeders[i+1:]...)
}

func (bbf *BucketBrigadeFeeder) Get() (packet Packet) {
	packet = bbf.gopCacheSnapshot.Get(bbf.pktFeed)
	return
}
