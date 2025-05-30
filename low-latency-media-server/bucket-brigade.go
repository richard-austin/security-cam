package main

import (
	log "github.com/sirupsen/logrus"
	"sync"
	"time"
)

// BucketBrigade /** A cache used to delay the video so that recordings triggered by camara's FTP upload have some preamble
//
//	/** to the point at which action was detected
type BucketBrigade struct {
	feeders     []*BucketBrigadeFeeder
	cacheLength int
	mutex       sync.Mutex
	delayTime   int
	startTime   time.Time
	inputIndex  int
	indexLimit  int
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

func NewBucketBrigade(preambleTime int) (bucketBrigade BucketBrigade) {
	const cacheLength = 600
	bucketBrigade = BucketBrigade{
		Cache:       make([]Packet, cacheLength),
		feeders:     make([]*BucketBrigadeFeeder, 0),
		started:     false,
		delayTime:   preambleTime + 1,
		inputIndex:  0,
		indexLimit:  0,
		cacheLength: cacheLength,
		gopCache:    NewGopCache(true)}
	return
}

func (bb *BucketBrigade) isReady() (ready bool) {
	ready = bb.started
	return
}

func (bb *BucketBrigade) Input(p Packet) (err error) {
	err = nil
	bb.mutex.Lock()
	defer bb.mutex.Unlock()

	bb.Cache[bb.inputIndex] = p
	if !bb.started {
		if bb.startTime.IsZero() {
			bb.startTime = time.Now()
		}
		bb.inputIndex++
		if bb.inputIndex >= bb.cacheLength || time.Since(bb.startTime) > time.Duration(bb.delayTime)*time.Second {
			if bb.inputIndex >= bb.cacheLength {
				log.Warnf("Input index %d >= cache length %d: Using bucket brigade cachLength as indexLimit", bb.inputIndex, bb.cacheLength)
				bb.indexLimit = bb.cacheLength
			}
			bb.started = true
			bb.indexLimit = bb.inputIndex
			bb.inputIndex = 0
		}
		return
	} else {
		opIdx := (bb.inputIndex + 1) % (bb.indexLimit + 1)
		err = bb.gopCache.RecordingInput(bb.Cache[opIdx])
		for _, f := range bb.feeders {
			select {
			case f.pktFeed <- bb.Cache[opIdx]:
			default:
				log.Errorf("Missed packet in bucket brigade Input: %d items in pktFeed channel\n", len(f.pktFeed))
			}
		}
	}
	if bb.inputIndex < bb.indexLimit {
		bb.inputIndex++
	} else {
		bb.inputIndex = 0
	}

	return
}

func (bb *BucketBrigade) CreateFeeder() (bbFeeder *BucketBrigadeFeeder) {
	bb.mutex.Lock()
	defer bb.mutex.Unlock()
	bbFeeder = &BucketBrigadeFeeder{
		gopCacheSnapshot: bb.gopCache.GetSnapshot(),
		pktFeed:          make(chan Packet, 300),
	}

	bb.feeders = append(bb.feeders, bbFeeder)
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
	bb.feeders[i] = bb.feeders[len(bb.feeders)-1]
	bb.feeders = bb.feeders[:len(bb.feeders)-1]
}

func (bbf *BucketBrigadeFeeder) Get() (packet Packet) {
	packet = bbf.gopCacheSnapshot.Get(bbf.pktFeed)
	return
}
