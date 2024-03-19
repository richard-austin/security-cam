package main

import (
	"encoding/binary"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"golang.org/x/net/websocket"
	"io"
	"net/http"
	"time"
)

var streams = NewStreams()

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

func serveHTTP(cameras *Cameras) {
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
			"suuidMap":            suuids,
			"suuid":               firstStream,
			"defaultLatencyLimit": config.DefaultLatencyLimit,
		})
	})

	// For web page with suuid
	router.GET("/:suuid", func(c *gin.Context) {

		c.HTML(http.StatusOK, "index.gohtml", gin.H{
			"suuidMap":            suuids,
			"suuid":               c.Param("suuid"),
			"defaultLatencyLimit": config.DefaultLatencyLimit,
		})
	})
	// For ffmpeg to write to
	router.POST("/live/:suuid", func(c *gin.Context) {
		req := c.Request
		suuid := req.FormValue("suuid")
		_, hasEntry := streams.StreamMap[suuid]
		if hasEntry {
			log.Errorf("Cannot add %s, there is already an existing stream with that id", suuid)
			return
		}
		log.Infof("Input connected for %s", suuid)
		readCloser := req.Body

		streams.addStream(suuid)
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
		if err == nil {
			err, _ := streams.getCodecsFromMoov(suuid)
			if err != nil {
				return
			}
		}
		// Empty the queue
		for len(queue) > 0 {
			_ = <-queue
		}
		for {
			data = data[:33000]
			numOfByte, err = readCloser.Read(data)
			if err != nil {
				log.Errorf("Error reading the data feed for stream %s:- %s", suuid, err.Error())
				break
			}
			d = NewPacket(data[:numOfByte])

			if err != nil {
				log.Error(err)
			}
			err = streams.put(suuid, d)

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
	// This does not send the codec info ahead of ftyp and moov
	router.GET("/h/:suuid", func(c *gin.Context) {
		ServeHTTPStream(c.Writer, c.Request)
	})

	// For websocket connections from mse
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
func ServeHTTPStream(w http.ResponseWriter, r *http.Request) {
	defer func() { r.Close = true }()
	suuid := r.FormValue("suuid")

	log.Infof("http Request %s", suuid)
	err, data := streams.getFtyp(suuid)
	if err != nil {
		log.Errorf("Error getting ftyp: %s", err.Error())
		return
	}
	nBytes, err := w.Write(data.pckt)
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
	if err != nil {
		log.Errorf("Error writing moov: %s", err.Error())
		return
	}
	log.Tracef("Sent moov through http to %s:- %d nBytes", suuid, nBytes)

	stream := streams.StreamMap[suuid]
	bb := stream.bucketBrigade.GetFeeder()
	defer stream.bucketBrigade.DestroyFeeder(bb)
	for {
		var data Packet
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

func ws(ws *websocket.Conn) {
	defer func() {
		err := ws.Close()
		if err != nil {
			log.Warnf("closing websocket:- %s", err.Error())
		}
	}()
	suuid := ws.Request().FormValue("suuid")

	log.Infof("ws Request %s", suuid)
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

	// Send the header information (codecs, ftyp and moov)
	var data Packet
	err, data = streams.getCodecs(suuid)
	if err != nil {
		log.Errorf("Error getting codecs: %s", err.Error())
		return
	}
	err = websocket.Message.Send(ws, data.pckt)
	if err != nil {
		log.Errorf("Error writing codecs: %s", err.Error())
		return
	}
	log.Tracef("Sent codecs through to %s:- %s", suuid, string(data.pckt))

	err, data = streams.getFtyp(suuid)
	if err != nil {
		log.Errorf("Error getting ftyp: %s", err.Error())
		return
	}
	err = websocket.Message.Send(ws, data.pckt)
	if err != nil {
		log.Errorf("Error writing ftyp: %s", err.Error())
		return
	}
	log.Tracef("Sent ftyp through to %s:- %d bytes", suuid, len(data.pckt))

	err, data = streams.getMoov(suuid)
	if err != nil {
		log.Errorf("Error getting moov: %s", err.Error())
	}
	err = websocket.Message.Send(ws, data.pckt)
	if err != nil {
		log.Errorf("Error writing moov: %s", err.Error())
	}
	log.Tracef("Sent moov through to %s:- %d bytes", suuid, len(data.pckt))

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
	gopCache := stream.gopCache.GetSnapshot()
	gopCacheUsed := stream.gopCache.GopCacheUsed
	// Main loop to send moof and mdat atoms
	started := false
	for {
		if gopCacheUsed {
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
