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
	cacheInUse  int
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
	const cacheLength = 2500
	bucketBrigade = BucketBrigade{
		Cache:       make([]Packet, cacheLength),
		feeders:     make([]*BucketBrigadeFeeder, 0),
		started:     false,
		delayTime:   preambleTime,
		inputIndex:  0,
		cacheInUse:  0,
		cacheLength: cacheLength,
		gopCache:    NewGopCache(true, false)}
	return
}

func (bb *BucketBrigade) isReady() (ready bool) {
	ready = bb.started
	return
}
func mod(a, b int) int {
	return (a%b + b) % b
}

func (bb *BucketBrigade) SetCacheSizeFromDelayTime() {
	if bb.inputIndex+1 >= bb.cacheLength || time.Since(bb.startTime) > time.Duration(bb.delayTime)*time.Second {
		if bb.inputIndex+1 >= bb.cacheLength {
			log.Warnf("Input index %d >= cache length %d: Using bucket brigade cachLength as indexLimit", bb.inputIndex, bb.cacheLength)
			bb.cacheInUse = bb.cacheLength - 1
		}

		bb.started = true
		bb.cacheInUse = bb.inputIndex
		log.Infof("Bucket brigade cache size set to %d", bb.cacheInUse)
	}
}
func (bb *BucketBrigade) Input(p Packet) (err error) {
	err = nil
	bb.mutex.Lock()
	defer bb.mutex.Unlock()

	bb.Cache[bb.inputIndex] = p
	if bb.inputIndex == 0 && !bb.started {
		bb.startTime = time.Now()
	}
	if !bb.started { // Find the cacheInUse size if settingUp set
		bb.SetCacheSizeFromDelayTime()
		bb.inputIndex++
		return
	}
	opIdx := mod(bb.inputIndex-bb.cacheInUse, bb.cacheLength)
	err = bb.gopCache.RecordingInput(bb.Cache[opIdx])
	//log.Infof("inputIndex = %d, cacheInUse = %d, opIdx = %d,", bb.inputIndex, bb.cacheInUse, opIdx)
	if err != nil {
		log.Errorf("Error in gop cache: %s", err.Error())
		return
	}
	for _, f := range bb.feeders {
		select {
		case f.pktFeed <- bb.Cache[opIdx]:
		default:
			log.Errorf("Missed packet in bucket brigade Input: %d items in pktFeed channel\n", len(f.pktFeed))
		}
	}

	if bb.inputIndex < bb.cacheLength-1 {
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
