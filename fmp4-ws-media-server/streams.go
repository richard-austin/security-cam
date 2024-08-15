package main

import (
	"bytes"
	"crypto/rand"
	"encoding/binary"
	"fmt"
	log "github.com/sirupsen/logrus"
	"sync"
)

type Stream struct {
	ftyp          Packet
	moov          Packet
	codecs        string
	gopCache      GopCache
	bucketBrigade BucketBrigade
	PcktStreams   map[string]*PacketStream // One packetStream for each client connected through the suuid
}
type StreamMap map[string]*Stream
type Streams struct {
	mutex sync.RWMutex
	StreamMap
}

func NewStreams() *Streams {
	s := Streams{}
	s.StreamMap = StreamMap{}

	return &s
}

func (s *Streams) addStream(suuid string) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	streamC, err := getStreamC(suuid)
	if err != nil {
		log.Errorf("could not get a camera stream for suuid %s\n", suuid)
	}
	s.StreamMap[suuid] = &Stream{PcktStreams: map[string]*PacketStream{}, gopCache: NewGopCache(config.GopCache), bucketBrigade: NewBucketBrigade(streamC.PreambleFrames)}
}

func (s *Streams) removeStream(suuid string) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	delete(s.StreamMap, suuid)
}

func (s *Streams) addClient(suuid string) (cuuid string, pkt chan Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()

	stream, ok := s.StreamMap[suuid]
	if ok {
		cuuid = pseudoUUID()
		pktStream := NewPacketStream()
		stream.PcktStreams[cuuid] = &pktStream
		pkt = pktStream.ps
	} else {
		pkt = nil
	}
	return
}

func (s *Streams) deleteClient(suuid string, cuuid string) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	stream, ok := s.StreamMap[suuid]
	if ok {
		delete(stream.PcktStreams, cuuid)
	}
}

func (s *Streams) put(suuid string, pckt Packet) error {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	var retVal error = nil
	stream, ok := s.StreamMap[suuid]
	if ok {
		err := stream.gopCache.Input(pckt)
		if err != nil {
			_ = fmt.Errorf(err.Error())
		}
		err = stream.bucketBrigade.Input(pckt)
		if err != nil {
			_ = fmt.Errorf(err.Error())
		}
		for _, packetStream := range stream.PcktStreams {
			length := len(packetStream.ps)
			log.Tracef("%s channel length = %d", suuid, length)
			select {
			case packetStream.ps <- pckt:
			default:
				{
					retVal = fmt.Errorf("client channel for %s has reached capacity (%d)", suuid, length)
				}
			}
		}

	} else {
		retVal = fmt.Errorf("no stream with name %s was found", suuid)
	}
	return retVal
}

func (s *Streams) putFtyp(suuid string, pckt Packet) (retVal error) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	retVal = nil
	// Check it is actually a ftyp
	val := getSubBox(pckt, "ftyp")
	if val == nil {
		retVal = fmt.Errorf("The packet recieved in putFtyp was not a ftyp")
		return
	} else {
		stream, ok := s.StreamMap[suuid]
		if ok {
			stream.ftyp = pckt
			s.StreamMap[suuid] = stream
		} else {
			retVal = fmt.Errorf("Stream %s not found", suuid)
		}
	}
	return
}

func (s *Streams) putMoov(suuid string, pckt Packet) (retVal error) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	retVal = nil
	// Check it is actually a moov
	val := getSubBox(pckt, "moov")
	if val == nil {
		retVal = fmt.Errorf("The packet recieved in putMoov was not a moov")
		return
	} else {
		stream, ok := s.StreamMap[suuid]
		if ok {
			stream.moov = pckt
			s.StreamMap[suuid] = stream
		} else {
			retVal = fmt.Errorf("Stream %s not found", suuid)
		}
	}
	return
}

/** getStreamC: Get camera stream for the http stream suuid
 */
func getStreamC(suuid string) (streamC StreamC, err error) {
	err = nil
	for _, camera := range cameras.Cameras {
		stream, ok := camera.Streams[suuid]
		if ok {
			streamC = stream
			return
		}
	}
	err = fmt.Errorf("no stream found for suuid %s", suuid)
	return
}

func (s *Streams) getCodecs(suuid string) (err error, pckt Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	pckt.pckt = append([]byte{0x09}, []byte(s.StreamMap[suuid].codecs)...)
	err = nil
	return
}

func (s *Streams) getFtyp(suuid string) (error, Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	var retVal error = nil
	stream, ok := s.StreamMap[suuid]
	if !ok {
		retVal = fmt.Errorf("Stream %s not found", suuid)
	} else if stream.ftyp.pckt == nil {
		retVal = fmt.Errorf("No ftyp for stream %s", suuid)
	}
	return retVal, stream.ftyp

}

func (s *Streams) getMoov(suuid string) (error, Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	var retVal error = nil
	stream, ok := s.StreamMap[suuid]
	if !ok {
		retVal = fmt.Errorf("stream %s not found", suuid)
	} else if stream.moov.pckt == nil {
		retVal = fmt.Errorf("no moov for stream %s", suuid)
	}

	return retVal, stream.moov
}

func (s *Streams) getCodecsFromMoov(suuid string) (err error, codecs string) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	if s.StreamMap[suuid].moov.pckt == nil {
		err = fmt.Errorf("cannot get codecs, no moov data")
		return
	}
	names := []string{"trak", "mdia", "minf", "stbl", "stsd", "avc1", "avcC"}
	hevcNames := []string{"trak", "mdia", "minf", "stbl", "stsd", "hev1", "hvcC"}

	val := s.StreamMap[suuid].moov.pckt
	// Find the video codec data
	trakLen := 0

	for i, n := range names {
		val = getSubBox(Packet{val}, n)
		if val != nil {
			log.Tracef("Found %s", n)
			if i == 0 {
				// Save the length of the trak
				trakLen = int(binary.BigEndian.Uint32(val[:4]))
			}
		} else {
			log.Warnf("No %s in moov when looking for avc1 codec data", n)
			break
		}
	}
	// If avc1 codec found
	if val != nil {
		// Save the codec data in hex string format as required by mse
		codecs = fmt.Sprintf("avc1.%02x%02x%02x", val[9], val[10], val[11])
	} else {
		// See if we can find h265 (hevc) codec data
		val = s.StreamMap[suuid].moov.pckt
		for i, n := range hevcNames {
			val = getSubBox(Packet{val}, n)
			if val != nil {
				log.Tracef("Found %s", n)
				if i == 0 {
					// Save the length of the trak
					trakLen = int(binary.BigEndian.Uint32(val[:4]))
				}
			} else {
				log.Errorf("No %s in moov when looking for HEVC codec info", n)
				break
			}
		}
		// If video codec found
		if val != nil {
			// Save the codec data in hex string format as required by mse
			codecs = fmt.Sprintf("hvc1.%d.4.L%d.B0", val[9]&0x1f, val[21]&0x1f)
		} else {
			log.Errorln("No video codec found for", suuid)
			err = fmt.Errorf("no video codec found for %s", suuid)
		}
	}

	// Find audio codec data (if present)
	val = s.StreamMap[suuid].moov.pckt[trakLen:]
	names2 := []string{"trak", "mdia", "minf", "stbl", "stsd", "mp4a", "esds"}

	for _, n := range names2 {
		val = getSubBox(Packet{val}, n)
		if val != nil {
			log.Tracef("Found %s", n)
		} else {
			log.Tracef("No second %s in moov. No audio present", n)
			break
		}
	}

	if val != nil {
		// Audio stream present
		aacCodec := val[25:27]
		aacCodec[1] &= 0x0f // Mask off the high nybble
		codecs += fmt.Sprintf(", mp4a.%2x.%x", aacCodec[0], aacCodec[1])
	}

	stream := s.StreamMap[suuid]
	stream.codecs = codecs
	s.StreamMap[suuid] = stream
	return
}

type PacketStream struct {
	ps chan Packet
	//	gopCacheSnapshot *GopCacheSnapshot
}

func NewPacketStream() (packetStream PacketStream) {
	packetStream = PacketStream{ps: make(chan Packet, 300)}
	return
}

type Packet struct {
	pckt []byte
}

func NewPacket(pckt []byte) Packet {
	b := make([]byte, len(pckt))
	copy(b, pckt)
	return Packet{pckt: b}
}

func (p Packet) isKeyFrame() (retVal bool) {
	// [moof [mfhd] [traf [tfhd] [tfdt] [moof]]]
	retVal = false
	moof := getSubBox(p, "moof")
	if moof == nil {
		log.Warnf("moof was nil in isKeyFrame")
		return
	}
	flags := moof[3:5]

	retVal = flags[0] == 0x68 || flags[0] == 0xb4
	//	log.Infof("flags = 0x%x%c, %t", flags[0], flags[1], retVal)
	return
}

func (p Packet) isMoof() (retVal bool) {
	retVal = false
	if len(p.pckt) > 20 {
		moof := getSubBox(p, "moof")
		retVal = moof != nil
	}
	return
}

func getSubBox(pckt Packet, boxName string) (sub_box []byte) {
	searchData := pckt.pckt
	searchTerm := []byte(boxName)
	idx := bytes.Index(searchData, searchTerm)

	if idx >= 4 {
		length := int(binary.BigEndian.Uint32(searchData[idx-4 : idx]))
		sub_box = searchData[idx-4 : length+idx-4]

	} else {
		sub_box = nil
	}
	return
}

func pseudoUUID() (uuid string) {
	const pseudoUUIDLen int = 16
	b := make([]byte, pseudoUUIDLen)
	_, err := rand.Read(b)
	if err != nil {
		log.Errorf("Error in pseudoUUID: %s", err.Error())
		return
	}
	uuid = fmt.Sprintf("%X-%X-%X-%X-%X", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
	return
}
