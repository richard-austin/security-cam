package main

import (
	"bytes"
	"crypto/rand"
	"fmt"
	log "github.com/sirupsen/logrus"
	"strings"
	"sync"
)

type Stream struct {
	flvHeader     Packet
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

func (s *Streams) addStream(suuid string, isAudio bool) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	gopCacheUsed := config.GopCache
	if isAudio {
		gopCacheUsed = false
	}
	stream := &Stream{PcktStreams: map[string]*PacketStream{}, gopCache: NewGopCache(gopCacheUsed)}
	s.StreamMap[suuid] = stream
}

func (s *Streams) addRecordingStream(suuid string) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	/*streamC*/ _, err := getStreamC(suuid)
	if err != nil {
		log.Errorf("Failed to get streamC: %v", err)
	}
	streamC, err := getStreamC(suuid)
	if err != nil {
		log.Errorf("Failed to get streamC: %v, Cannot add recording stream", err)
	} else {
		s.StreamMap[suuid] = &Stream{PcktStreams: map[string]*PacketStream{}, gopCache: NewGopCache(config.GopCache), bucketBrigade: NewBucketBrigade(streamC.PreambleTime)}
	}
}

/** getStreamC: Get camera stream for the fmp4 http stream suuid
 */
func getStreamC(suuid string) (streamC StreamC, err error) {
	err = nil
	log.Tracef("suuid: %s", suuid)
	if len(suuid) > 3 {
		dashPos := strings.Index(suuid, "-")
		if dashPos != -1 {
			camLen := len("cam")
			camNum := suuid[camLen:dashPos]
			camName := "camera" + camNum
			log.Tracef("camera: %s", camName)
			cam, ok := cameras.Cameras[camName]
			if ok {
				streamName, _ := strings.CutSuffix(suuid[dashPos+1:], "r") // Remove the "r" recording suffix to get the stream name
				log.Tracef("Stream name: %s", streamName)

				stream, ok := cam.Streams[streamName]
				if ok {
					return stream, err
				} else {
					err = fmt.Errorf("stream %s not found", streamName)
				}
			} else {
				err = fmt.Errorf("camera %s not found", camName)
			}
		} else {
			err = fmt.Errorf("cannot find dash in stream nameq: %s", suuid)
		}
	}
	return
}

func (s *Streams) removeStream(suuid string) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	_, ok := s.StreamMap[suuid]
	if ok {
		delete(s.StreamMap, suuid)
	}
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

func (s *Streams) put(suuid string, pckt Packet, isRecording ...bool) error {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	var retVal error = nil
	stream, ok := s.StreamMap[suuid]
	if ok {
		if len(isRecording) > 0 && isRecording[0] {
			err := stream.bucketBrigade.Input(pckt)
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
			err := stream.gopCache.Input(pckt)
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
		}
	} else {
		retVal = fmt.Errorf("no stream with name %s was found", suuid)
	}
	return retVal
}

func (s *Streams) getGOPCache(suuid string) (err error, gopCache *GopCacheSnapshot) {
	gopCache = nil
	stream, ok := s.StreamMap[suuid]
	if !ok {
		err = fmt.Errorf("no stream for %s in getGOPCache", suuid)
		return
	}
	gopCache = stream.gopCache.GetSnapshot()
	return
}

func (s *Streams) getCodec(suuid string) (err error, pckt Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	avi, err := codecs.getCodecString(suuid)
	if avi != nil {
		pckt.pckt = append([]byte{0x09}, []byte(avi.Codec)...)
	} else {
		err = fmt.Errorf("no codec for %s in getCodec", suuid)
	}
	return
}

type PacketStream struct {
	ps chan Packet
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

var hevcStart = []byte{0x00, 0x00, 0x01}
var h264Start = []byte{0x00, 0x00, 0x00, 0x01}
var h264KeyFrame1 = []byte{0x67, 0x64}
var h264KeyFrame2 = []byte{0x27, 0x64}
var h264KeyFrame3 = []byte{0x61, 0x88}

func (p Packet) isKeyFrame() (retVal bool) {
	retVal = false
	if bytes.Equal(p.pckt[:len(h264Start)], h264Start) {
		// H264 header
		retVal = bytes.Equal(p.pckt[4:6], h264KeyFrame1)
		if !retVal {
			retVal = bytes.Equal(p.pckt[4:6], h264KeyFrame2)
		}
		if !retVal {
			retVal = bytes.Equal(p.pckt[4:6], h264KeyFrame3)
		}
	} else if bytes.Equal(p.pckt[:len(hevcStart)], hevcStart) {
		// HEVC header
		theByte := p.pckt[3]
		retVal = theByte == 0x40
		theByte = (theByte >> 1) & 0x3f
		retVal = theByte == 0x19 || theByte == 0x20
	}
	return
}
func (p Packet) isFlvKeyFrame() (retVal bool) {
	retVal = false
	tag := p.pckt
	// Check if it is a keyframe
	if len(tag) >= 12 && tag[0]&0x1f == 9 && (tag[11]&0x70)>>4 == 1 {
		retVal = true
	}
	return
}

func (s *Streams) putFlvHeader(suuid string, pckt Packet) (retVal error) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	retVal = nil
	// Check it is actually a flv header
	if !bytes.Equal(pckt.pckt[:13], []byte{0x46, 0x4c, 0x56, 1, 5, 0, 0, 0, 9, 0, 0, 0, 0}) {
		retVal = fmt.Errorf("the packet recieved in putFlvHeader was not a flv header")
		return
	} else {
		stream, ok := s.StreamMap[suuid]
		if ok {
			stream.flvHeader = pckt
			s.StreamMap[suuid] = stream
		} else {
			retVal = fmt.Errorf("stream %s not found", suuid)
		}
	}
	return
}

func (s *Streams) getFlvHeader(suuid string) (err error, flvHeader Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	err = nil
	stream, ok := s.StreamMap[suuid]

	log.Infof("ok = %t", ok)
	if !ok {
		err = fmt.Errorf("stream %s not found", suuid)
	} else if stream.flvHeader.pckt == nil {
		err = fmt.Errorf("no flv header for stream %s", suuid)
	} else {
		flvHeader = stream.flvHeader
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
