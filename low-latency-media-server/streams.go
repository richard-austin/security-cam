package main

import (
	"bytes"
	"crypto/rand"
	"encoding/json"
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

/** getStreamC: Get camera stream for the flv http stream suuid
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

func (s *Streams) hasEntry(suuid string) (hasEntry bool) {
	s.mutex.RLock()
	defer s.mutex.RUnlock()
	_, hasEntry = streams.StreamMap[suuid]
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

func (s *Streams) put(suuid string, pckt Packet, isRecording ...bool) (retVal error) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	retVal = nil
	stream, ok := s.StreamMap[suuid]
	if ok {
		if len(isRecording) > 0 && isRecording[0] {
			err := stream.bucketBrigade.Input(pckt)
			if err != nil {
				retVal = fmt.Errorf(err.Error())
				return retVal
			}
			for _, packetStream := range stream.PcktStreams {
				length := len(packetStream.ps)
				log.Tracef("%s channel length = %d", suuid, length)
				select {
				case packetStream.ps <- pckt:
				default:
					{
						retVal = fmt.Errorf("client channel for %s has reached capacity (%d)", suuid, length)
						return retVal
					}
				}
			}
		} else {
			err := stream.gopCache.Input(pckt)
			if err != nil {
				retVal = fmt.Errorf(err.Error())
				return retVal
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

func (s *Streams) getVideoCodec(suuid string) (err error, pckt Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	avi, err := codecs.getAVCodecs(suuid)
	if avi != nil {
		pckt.pckt = append([]byte{0x09}, []byte(avi.Codec)...)
	} else {
		err = fmt.Errorf("no codec for %s in getVideoCodec", suuid)
	}
	return
}

func (s *Streams) getAudioCodec(suuid string) (err error, pckt Packet) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	baseSuuid, done := strings.CutSuffix(suuid, "a")
	if !done {
		err = fmt.Errorf("invalid suuid %s", suuid)
	}
	avi, err := codecs.getAVCodecs(baseSuuid)
	if avi != nil {
		var audioCodec []byte
		audioCodec, err = json.Marshal(avi.AudioInfo)
		if err != nil {
			log.Errorf("Error marshalling audio codec: %s", err.Error())
			return
		}
		log.Tracef("Audio codec: %s", audioCodec)
		pckt.pckt = append([]byte{0x09}, audioCodec...)
	} else {
		err = fmt.Errorf("no codec for %s in getAudioCodec", suuid)
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

func (p Packet) isFLVHeader() (retVal bool, size int32) {
	retVal = false
	if len(p.pckt) > 13 {
		if bytes.Equal(p.pckt[:14], []byte{'F', 'L', 'V', 1, 5, 0, 0, 0, 9, 0, 0, 0, 0, 18}) ||
			bytes.Equal(p.pckt[:14], []byte{'F', 'L', 'V', 1, 1, 0, 0, 0, 9, 0, 0, 0, 0, 18}) {
			retVal = true
			// Also return the total size of the FLV header packets
			size = int32(9)         // Size of the actual header
			payloadSize := int32(5) // Initial size > 4 to ensure the loop is executed
			pktTypes := make(map[int32]int32)
			for size < int32(len(p.pckt)-4) && payloadSize > 4 {
				packetType := int32(p.pckt[size+4])
				_, found := pktTypes[packetType]
				if found {
					//log.Infof("Packet type = %d already found", packetType)
					break
				} else {
					pktTypes[packetType] = packetType
				}

				payloadSize = int32(p.pckt[size+5])*65536 + int32(p.pckt[size+6])*256 + int32(p.pckt[size+7]) // Size of next packet payload
				size += payloadSize + 15
			}
			size += 4 // Has int32 size of previous packet at the end
		}
	}
	return
}

var hevcStart = []byte{0x00, 0x00, 0x01}
var h264Start = []byte{0x00, 0x00, 0x00, 0x01}
var h264KeyFrame1 = []byte{0x67, 0x64}
var h264KeyFrame2 = []byte{0x27, 0x64}
var h264KeyFrame3 = []byte{0x61, 0x88}

func (p Packet) isKeyFrame() (retVal bool) {
	retVal = false
	if len(p.pckt) > 7 {
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
	}
	return
}
func (p Packet) isFlvKeyFrame() (retVal bool) {
	retVal = false
	tag := p.pckt
	// Check if it is a keyframe
	if len(tag) >= 12 {
		ft := (tag[11] & 0xf0) >> 4 // ft == 1 for h264 or 9 for hevc keyframes
		if tag[0]&0x1f == 9 && (ft == 1 || ft == 9) {
			retVal = true
		}
	}
	return
}

func (s *Streams) putFlvHeader(suuid string, pckt Packet) (retVal error) {
	s.mutex.Lock()
	defer s.mutex.Unlock()
	retVal = nil
	// Check it is actually a flv header
	if !bytes.Equal(pckt.pckt[:13], []byte{0x46, 0x4c, 0x56, 1, 5, 0, 0, 0, 9, 0, 0, 0, 0}) &&
		!bytes.Equal(pckt.pckt[:13], []byte{0x46, 0x4c, 0x56, 1, 1, 0, 0, 0, 9, 0, 0, 0, 0}) {
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

	if !ok {
		err = fmt.Errorf("stream %s not found", suuid)
	} else if stream.flvHeader.pckt == nil {
		err = fmt.Errorf("no flv header for stream %s", suuid)
	} else {
		flvHeader = stream.flvHeader
	}
	return
}

func (s *Streams) getStream(suuid string) (stream *Stream, ok bool) {
	s.mutex.RLock()
	defer s.mutex.RUnlock()
	stream, ok = s.StreamMap[suuid]
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
