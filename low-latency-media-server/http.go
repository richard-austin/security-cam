package main

import (
	"encoding/binary"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"golang.org/x/net/websocket"
	"io"
	"net/http"
	"strings"
	"time"
)

var streams = NewStreams()

func serveHTTP() {
	router := gin.Default()
	gin.SetMode(gin.DebugMode)
	router.LoadHTMLFiles("web/index.gohtml")
	suuids := cameras.Suuids()
	// Get the name of the first stream
	var first Camera
	var firstStream string
	for _, first = range cameras.Cameras {
		for firstStream = range first.Streams {
			break
		}
		break
	}
	// For web page without suuid
	router.GET("/", func(c *gin.Context) {

		c.HTML(http.StatusOK, "index.gohtml", gin.H{
			"suuidMap": suuids,
			"suuid":    firstStream,
		})
	})

	// For web page with suuid
	router.GET("/:suuid", func(c *gin.Context) {

		c.HTML(http.StatusOK, "index.gohtml", gin.H{
			"suuidMap": suuids,
			"suuid":    c.Param("suuid"),
		})
	})

	// For ffmpeg to write to for live-streaming (with suuid)
	router.POST("/live/:suuid", func(c *gin.Context) {
		req := c.Request
		suuid := req.FormValue("suuid")
		isAudio := strings.HasSuffix(suuid, "a")
		_, hasEntry := streams.StreamMap[suuid]
		if hasEntry {
			log.Errorf("Cannot add %s, there is already an existing stream with that id and media type", suuid)
			return
		}

		log.Infof("Input connected for %s", suuid)
		readCloser := req.Body

		streams.addStream(suuid, isAudio)
		defer streams.removeStream(suuid)
		data := make([]byte, 33000)

		t := time.NewTimer(5 * time.Second)
		for {
			data = data[:33000]
			numOfByte, err := readCloser.Read(data)
			if err != nil {
				log.Errorf("Error reading the data feed for stream %s:- %s", suuid, err.Error())
				break
			}
			d := NewPacket(data[:numOfByte])
			select {
			case <-t.C:
				err = fmt.Errorf("(timeout occurred)")
				break
			default:
				t.Reset(2 * time.Second)
				break
			}
			if err == nil {
				err = streams.put(suuid, d, false)
			}

			if err != nil {
				log.Errorf("Error putting the packet into stream %s:- %s", suuid, err.Error())
				break
			} else if numOfByte == 0 {
				break
			}
			log.Tracef("%d bytes received", numOfByte)
		}
	})

	// For ffmpeg to write to for recording (with rsuuid)
	router.POST("/recording/:rsuuid", func(c *gin.Context) {
		req := c.Request
		suuid := req.FormValue("rsuuid")
		_, hasEntry := streams.StreamMap[suuid]
		if hasEntry {
			log.Errorf("Cannot add %s, there is already an existing stream with that id", suuid)
			return
		}
		log.Infof("Input connected for %s", suuid)
		readCloser := req.Body

		streams.addRecordingStream(suuid)
		defer streams.removeStream(suuid)

		data := make([]byte, 33000)
		queue := make(chan Packet, 1)

		// Set up the stream ready for connection from client, put in the ftyp, moov and codec data
		numOfByte, err := ReadBox(readCloser, data, queue)
		if err != nil {
			log.Errorf("Error reading the ftyp data for stream %s:- %s", suuid, err.Error())
			return
		}

		d := NewPacket(data[:numOfByte]) //make([]byte, numOfByte)
		err = streams.putFtyp(suuid, d)
		if err != nil {
			return
		}

		numOfByte, err = ReadBox(readCloser, data, queue)
		if err != nil {
			log.Errorf("Error reading the moov data for stream %s:- %s", suuid, err.Error())
			return
		}

		d = NewPacket(data[:numOfByte])
		err = streams.putMoov(suuid, d)

		if err != nil {
			//err, _ := streams.getCodecsFromMoov(suuid)
			//if err != nil {
			return
			//}
		}
		// Empty the queue
		for len(queue) > 0 {
			_ = <-queue
		}

		t := time.NewTimer(5 * time.Second)
		for {
			data = data[:33000]
			numOfByte, err = readCloser.Read(data)
			if err != nil {
				log.Errorf("Error reading the data feed for stream %s:- %s", suuid, err.Error())
				break
			}
			d = NewPacket(data[:numOfByte])
			select {
			case <-t.C:
				err = fmt.Errorf("(timeout occurred)")
				break
			default:
				t.Reset(2 * time.Second)
				break
			}
			if err != nil {
				log.Errorf("Error reading the data feed for stream %s:- %s", suuid, err.Error())
				break
			}
			if err == nil {
				err = streams.put(suuid, d, true)
			}

			if err != nil {
				log.Errorf("Error putting the packet into stream %s:- %s", suuid, err.Error())
				break
			} else if numOfByte == 0 {
				break
			}
			log.Tracef("%d bytes received", numOfByte)
		}
	})

	router.StaticFS("/web", http.Dir("web"))

	// For http connections from ffmpeg to read from (for recordings)
	// This is the mpegts stream
	router.GET("/h/:rsuuid", func(c *gin.Context) {
		ServeHTTPStream(c.Writer, c.Request)
	})

	// For websocket connections
	router.GET("/ws/:suuid", func(c *gin.Context) {
		handler := websocket.Handler(ws)
		handler.ServeHTTP(c.Writer, c.Request)
	})

	addr := fmt.Sprintf(":%d", config.ServerPort)
	err := router.Run(addr)
	if err != nil {
		log.Errorln(err)
	}
}

//// ServeHTTPStream For recording from
//// Recording command example which seems to work best. The tempo filter compensates for the tempo filter used to keep libe audio and video in sync:-
//// ffmpeg -y -use_wallclock_as_timestamps 1 -f hevc -thread_queue_size 2048 -i http://192.168.1.207:8081/h/stream?rsuuid=cam1-stream1 -thread_queue_size 2048 -use_wallclock_as_timestamps 1 -f alaw -i http://192.168.1.207:8081/h/stream?rsuuid=cam1-stream1a -f mp4 -c:v copy -c:a aac vid.mp4
//func ServeHTTPStream(w http.ResponseWriter, r *http.Request) {
//	log.Info("In ServeHTTPStream")
//
//	defer func() { r.Close = true }()
//	suuid := r.FormValue("suuid")
//
//	log.Infof("Request %s", suuid)
//	_, isAudio := strings.CutSuffix(suuid, "a")
//
//	cuuid, ch := streams.addClient(suuid)
//	if ch == nil {
//		return
//	}
//	log.Infof("number of cuuid's = %d", len(streams.StreamMap[suuid].PcktStreams))
//	defer streams.deleteClient(suuid, cuuid)
//
//	stream := streams.StreamMap[suuid]
//	var gopCache *GopCacheSnapshot
//	if !isAudio { // Audio GOP cache not used for live streams, only recordings
//		gopCache = stream.gopCache.GetSnapshot()
//	}
//	gopCacheUsed := stream.gopCache.GopCacheUsed
//
//	started := isAudio
//	for {
//		var data Packet
//		if gopCacheUsed && !isAudio {
//			data = gopCache.Get(ch)
//			started = true
//		} else {
//			data = <-ch
//			if !started {
//				if isAudio {
//					started = true
//				}
//				if data.isKeyFrame() {
//					started = true
//				} else {
//					continue
//				}
//			}
//		}
//		// See https://en.wikipedia.org/wiki/MPEG_transport_stream
//		//	log.Infof("Length = %d %02x", len(data.pckt), data.pckt)
//		numbytes, err := w.Write(data.pckt)
//		if err != nil {
//			// Warning only as it could be because the client disconnected
//			log.Warnf("writing to client for %s:= %s", suuid, err.Error())
//			break
//		}
//		log.Tracef("Data sent to http client for %s:- %d bytes", suuid, numbytes)
//	}
//}

// ServeHTTPStream For recording from
// Recording command example which seems to work well.
// ffmpeg -y -f mp4 -i http://localhost:8085/h/stream?rsuuid=cam1-stream1r -f mp4 test.mp4
func ServeHTTPStream(w http.ResponseWriter, r *http.Request) {
	log.Info("In ServeHTTPStream")

	defer func() { r.Close = true }()
	suuid := r.FormValue("rsuuid")

	log.Infof("http Request %s", suuid)
	err, data := streams.getFtyp(suuid)
	if err != nil {
		log.Errorf("Error getting ftyp: %s", err.Error())
		return
	}
	nBytes, err := w.Write(data.pckt)
	log.Tracef("ftyp = %s", string(data.pckt))
	if err != nil {
		log.Errorf("Error writing ftyp: %s", err.Error())
		return
	}
	log.Tracef("Sent ftyp through http to %s:- %d nBytes", suuid, nBytes)

	err, data = streams.getMoov(suuid)
	if err != nil {
		log.Errorf("Error getting moov: %s", err.Error())
		return
	}
	nBytes, err = w.Write(data.pckt)
	log.Tracef("moov = %s", string(data.pckt))
	if err != nil {
		log.Errorf("Error writing moov: %s", err.Error())
		return
	}
	log.Tracef("Sent moov through http to %s:- %d nBytes", suuid, nBytes)
	time.Sleep(3 * time.Second)
	stream := streams.StreamMap[suuid]
	bb := stream.bucketBrigade.GetFeeder()
	defer stream.bucketBrigade.DestroyFeeder(bb)
	log.Infof("Bucket brigade cache size for %s = %d", suuid, stream.bucketBrigade.cacheLength)
	for {
		data = bb.Get()
		bytes, err := w.Write(data.pckt)
		if err != nil {
			// Warning only as it could be because the client disconnected
			log.Warnf("writing to client for %s:= %s", suuid, err.Error())
			break
		}
		log.Tracef("Data sent to http client for %s:- %d nBytes", suuid, bytes)
	}
}

// ws For live-streaming connection
func ws(ws *websocket.Conn) {
	defer func() {
		err := ws.Close()
		log.Warn("Closing the websocket")
		if err != nil {
			log.Warnf("closing websocket:- %s", err.Error())
		}
	}()
	suuid := ws.Request().FormValue("suuid")
	isAudio := strings.HasSuffix(suuid, "a")

	log.Infof("Request %s", suuid)
	err := ws.SetWriteDeadline(time.Now().Add(10 * time.Second))
	if err != nil {
		log.Errorf("Error in SetWriteDeadline %s", err.Error())
		return
	}
	cuuid, ch := streams.addClient(suuid)
	if ch == nil {
		return
	}
	defer streams.deleteClient(suuid, cuuid)
	log.Infof("number of cuuid's = %d", len(streams.StreamMap[suuid].PcktStreams))

	// Send the header information (codec)
	var data Packet
	if !isAudio {
		err, data = streams.getCodec(suuid)
		if err != nil {
			log.Errorf("Error getting codecs: %s", err.Error())
			return
		}
		err = websocket.Message.Send(ws, data.pckt)
		if err != nil {
			log.Errorf("Error writing codec: %s", err.Error())
			return
		}
	}

	go func() {
		for {
			var message string
			err := websocket.Message.Receive(ws, &message)
			if err != nil {
				_ = ws.Close()
				return
			}
		}
	}()

	stream := streams.StreamMap[suuid]
	var gopCache *GopCacheSnapshot
	gopCache = stream.gopCache.GetSnapshot()
	gopCacheUsed := stream.gopCache.GopCacheUsed
	// Main loop to send data to the browser
	started := isAudio // Always started for audio as we don't wait for a keyframe
	for {
		if gopCacheUsed && !isAudio { // GOP cache not used for audio
			data = gopCache.Get(ch)
			started = true
		} else {
			data = <-ch
			if !started {
				if data.isKeyFrame() {
					started = true
				} else {
					continue
				}
			}
		}

		err = ws.SetWriteDeadline(time.Now().Add(10 * time.Second))
		if err != nil {
			log.Warnf("calling SetWriteDeadline:- %s", err.Error())
			return
		}
		err = websocket.Message.Send(ws, data.pckt)
		if err != nil {
			log.Warnf("calling Send:- %s", err.Error())
			return
		}
		log.Tracef("Data sent to client %d bytes", len(data.pckt))
	}
}

/*
 ReadBox
 Sometimes the ftyp and moov atoms at the start of the stream from ffmpeg  are combined in one packet. This
	function separates them if this occurs, so they can be put in their respective places and the moov
	atom analyes to get the codec data. THis is only used to handle those first two messages. From then on it doesn't
	matter if messages get appended to each other as they are going straight to mse (or ffmpeg for recordings)
*/

func ReadBox(readCloser io.ReadCloser, data []byte, queue chan Packet) (numOfByte int, err error) {
	numOfByte = 0
	if len(queue) > 0 {
		pckt := <-queue
		copy(data, pckt.pckt)
		var boxLen = binary.BigEndian.Uint32(data[:4])
		if boxLen > uint32(len(data)) {
			lenData := len(data)
			numOfByte, err = readCloser.Read(data[lenData:])
			if err != nil {
				return
			}
			numOfByte += lenData
		}
	} else {
		numOfByte, err = readCloser.Read(data[:cap(data)])
		if err != nil {
			return
		}
	}
	var boxLen = binary.BigEndian.Uint32(data[0:4])
	if boxLen < uint32(numOfByte) {
		// The moov atom is tagged onto ftyp
		var tmp = make([]byte, uint32(numOfByte)-boxLen)
		copy(tmp, data[boxLen:uint32(numOfByte)-boxLen])
		moovLen := binary.BigEndian.Uint32(tmp[0:4])
		if int(moovLen) > len(tmp) {
			log.Errorf("Error: moov length (%d) is greater then the length of the message containing it (%d)", moovLen, len(tmp))
			err = fmt.Errorf("error: moov length (%d) is greater then the length of the message containing it (%d)", moovLen, len(tmp))
			return
		}
		queue <- NewPacket(tmp[:moovLen])
		log.Infof("splitting packet boxLen = %d, numOfByte = %d\n", boxLen, numOfByte)
		data = data[:boxLen]
	}
	numOfByte = int(boxLen)
	return
}
