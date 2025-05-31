package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"golang.org/x/net/websocket"
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
				t.Reset(5 * time.Second)
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
		defer func() {
			log.Infof("Closing the readCloser for %s", suuid)
			err := readCloser.Close()
			if err != nil {
				log.Errorf("Error closing the readCloser for %s:- %s", suuid, err.Error())
			}
		}()
		streams.addRecordingStream(suuid)
		defer streams.removeStream(suuid)

		data := make([]byte, 33000)
		numOfByte, err := readCloser.Read(data)
		if err != nil {
			log.Errorf("Error reading the data feed for stream %s:- %s", suuid, err.Error())
			return
		}
		//// Set up the stream ready for connection from client, put in the flv header
		d := NewPacket(data[:numOfByte]) //make([]byte, numOfByte)
		err = streams.putFlvHeader(suuid, d)
		if err != nil {
			return
		}
		for {
			data = data[:33000]
			numOfByte, err := readCloser.Read(data)
			if err != nil {
				log.Errorf("Error reading the data feed for stream %s:- %s", suuid, err.Error())
				break
			}
			d := NewPacket(data[:numOfByte])
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
				log.Warnf("Empty packet recieved for %s, exiting", suuid)
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

// ServeHTTPStream For recording from
// Recording command example which seems to work well.
// ffmpeg -y -f mp4 -i http://localhost:8085/h/stream?rsuuid=cam1-stream1r -f mp4 test.mp4
func ServeHTTPStream(w http.ResponseWriter, r *http.Request) {
	log.Info("In ServeHTTPStream")

	defer func() { r.Close = true }()
	suuid := r.FormValue("rsuuid")
	log.Infof("http Request %s", suuid)
	stream := streams.StreamMap[suuid]
	if !stream.bucketBrigade.isReady() {
		log.Errorf("Bucket brigade is not ready for %s", suuid)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	err, data := streams.getFlvHeader(suuid)
	if err != nil {
		log.Errorf("Error getting flv header: %s", err.Error())
		w.WriteHeader(http.StatusFailedDependency)
		return
	}
	_, err = w.Write(data.pckt)
	log.Tracef("flvHeader = %s", string(data.pckt))
	if err != nil {
		log.Errorf("Error writing flv header: %s", err.Error())
		return
	}
	time.Sleep(3 * time.Second)
	bb := stream.bucketBrigade.CreateFeeder()
	defer stream.bucketBrigade.DestroyFeeder(bb)
	log.Infof("Bucket brigade cache size for %s = %d", suuid, stream.bucketBrigade.indexLimit)
	for {
		data := bb.Get()
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
		err, data = streams.getVideoCodec(suuid)
		if err != nil {
			log.Errorf("Error getting codecs: %s", err.Error())
			return
		}
		err = websocket.Message.Send(ws, data.pckt)
		if err != nil {
			log.Errorf("Error writing codec: %s", err.Error())
			return
		}
	} else {
		err, data = streams.getAudioCodec(suuid)
		if err != nil {
			log.Errorf("Error getting codecs: %s", err.Error())
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
		if gopCacheUsed && !isAudio { // GOP cache is not used for audio
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
