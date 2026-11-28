package main

import (
	"bytes"
	"context"
	"crypto/tls"
	"io"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"sync"
	"syscall"
	"time"

	"github.com/coder/websocket"
	"github.com/quic-go/quic-go"
	"github.com/quic-go/quic-go/http3"
	"github.com/quic-go/webtransport-go"
)

const (
	listenAddr    = ":8080"
	springBootURL = "ws://127.0.0.1:8080/ws/stream" // Internal Spring Boot 4 WebSocket endpoint
	allowedOrigin = "https://localhost:8080"        // Must match your frontend origin
	certFile      = "cert.pem"
	keyFile       = "key.pem"

	maxFrameSize      = 10 * 1024 * 1024 // 10 MB max message size
	certCacheDuration = 5 * time.Minute  // Reload certs from disk every 5 mins
	outboundQueueSize = 5                // Max frames buffered per client before dropping stale frames
)

var bufferPool = sync.Pool{
	New: func() any {
		// Pre-allocate 64KB per buffer to fit typical video frames without resizing
		return bytes.NewBuffer(make([]byte, 0, 64*1024))
	},
}

// certReloader caches the TLS certificate in memory and automatically reloads it
// when Certbot/Nginx renews the files on disk.
type certReloader struct {
	mu       sync.RWMutex
	cert     *tls.Certificate
	lastLoad time.Time
	certPath string
	keyPath  string
}

func newCertReloader(certPath, keyPath string) (*certReloader, error) {
	r := &certReloader{certPath: certPath, keyPath: keyPath}
	if _, err := r.getCertificate(nil); err != nil {
		return nil, err
	}
	return r, nil
}

func (r *certReloader) getCertificate(_ *tls.ClientHelloInfo) (*tls.Certificate, error) {
	r.mu.RLock()
	if r.cert != nil && time.Since(r.lastLoad) < certCacheDuration {
		defer r.mu.RUnlock()
		return r.cert, nil
	}
	r.mu.RUnlock()

	r.mu.Lock()
	defer r.mu.Unlock()

	// Double-check after acquiring write lock
	if r.cert != nil && time.Since(r.lastLoad) < certCacheDuration {
		return r.cert, nil
	}

	cert, err := tls.LoadX509KeyPair(r.certPath, r.keyPath)
	if err != nil {
		// If reload fails (e.g., mid-renewal), fall back to the last valid cert if available
		if r.cert != nil {
			log.Printf("warning: failed to reload TLS cert, keeping cached version: %v", err)
			return r.cert, nil
		}
		return nil, err
	}

	r.cert = &cert
	r.lastLoad = time.Now()
	return r.cert, nil
}

func main() {
	reloader, err := newCertReloader(certFile, keyFile)
	if err != nil {
		log.Fatalf("failed to load initial TLS certificate: %v", err)
	}

	mux := http.NewServeMux()

	wtServer := &webtransport.Server{
		H3: &http3.Server{
			Addr:    listenAddr,
			Handler: mux,
			TLSConfig: &tls.Config{
				GetCertificate: reloader.getCertificate,
				NextProtos:     []string{"h3"},
			},
			QUICConfig: &quic.Config{
				MaxIncomingUniStreams:          1000,
				InitialStreamReceiveWindow:     2 * 1024 * 1024,  // 2 MB
				MaxStreamReceiveWindow:         6 * 1024 * 1024,  // 6 MB
				InitialConnectionReceiveWindow: 4 * 1024 * 1024,  // 4 MB
				MaxConnectionReceiveWindow:     15 * 1024 * 1024, // 15 MB
				KeepAlivePeriod:                15 * time.Second,
			},
		},
		CheckOrigin: func(r *http.Request) bool {
			origin := r.Header.Get("Origin")
			return origin == allowedOrigin ||
				strings.HasPrefix(origin, "http://localhost:") ||
				strings.HasPrefix(origin, "https://localhost:")
		},
	}

	mux.HandleFunc("/webtransport", func(w http.ResponseWriter, r *http.Request) {
		handleWebTransport(w, r, wtServer)
	})

	// Graceful shutdown handling
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	go func() {
		log.Printf("WebTransport bridge listening on UDP %s -> %s", listenAddr, springBootURL)
		if err := wtServer.ListenAndServe(); err != nil {
			log.Printf("WebTransport server stopped: %v", err)
		}
	}()

	<-ctx.Done()
	log.Println("Shutting down WebTransport bridge...")
	_ = wtServer.Close()
}

func handleWebTransport(w http.ResponseWriter, r *http.Request, wtServer *webtransport.Server) {
	// 1. Forward Spring Security Session / Remember-Me cookies & Origin
	dialHeaders := http.Header{}
	if cookie := r.Header.Get("Cookie"); cookie != "" {
		dialHeaders.Set("Cookie", cookie)
	}
	if origin := r.Header.Get("Origin"); origin != "" {
		dialHeaders.Set("Origin", origin)
	}

	// 2. Preserve URL query parameters (e.g., ?streamId=123)
	targetURL := springBootURL
	if r.URL.RawQuery != "" {
		targetURL += "?" + r.URL.RawQuery
	}

	// 3. Authenticate against Spring Boot FIRST before accepting WebTransport
	wsConn, resp, err := websocket.Dial(r.Context(), targetURL, &websocket.DialOptions{
		HTTPHeader: dialHeaders,
	})
	if err != nil {
		status := http.StatusUnauthorized
		if resp != nil {
			status = resp.StatusCode
		}
		log.Printf("Spring Boot rejected handshake (HTTP %d): %v", status, err)
		http.Error(w, "Unauthorized", status)
		return
	} else {
		log.Printf("Handshake accepted")
	}

	// 4. Upgrade HTTP/3 request to WebTransport session
	wtSess, err := wtServer.Upgrade(w, r)
	if err != nil {
		wsConn.Close(websocket.StatusInternalError, "WebTransport upgrade failed")
		log.Printf("WebTransport upgrade failed: %v", err)
		return
	}

	go bridgeSession(wtSess, wsConn)
}

func bridgeSession(wtSess *webtransport.Session, wsConn *websocket.Conn) {
	defer wtSess.CloseWithError(0, "session closed")
	defer wsConn.Close(websocket.StatusNormalClosure, "bridge closed")

	wsConn.SetReadLimit(maxFrameSize)
	sessCtx := wtSess.Context()

	// Bounded channel preserves frame order and applies backpressure
	outbound := make(chan *bytes.Buffer, outboundQueueSize)

	// 1. Client -> Spring Boot: Forward incoming unidirectional streams
	go func() {
		for {
			stream, err := wtSess.AcceptUniStream(sessCtx)
			if err != nil {
				return
			}
			go forwardUniStreamToWS(sessCtx, stream, wsConn)
		}
	}()

	// 2. Dedicated Sender Goroutine: Sends frames in strict order and recycles buffers
	go func() {
		for {
			select {
			case <-sessCtx.Done():
				return
			case buf := <-outbound:
				sendPayloadOverUniStream(sessCtx, wtSess, buf.Bytes())
				buf.Reset()
				bufferPool.Put(buf)
			}
		}
	}()

	// 3. Spring Boot -> Bridge: Read into pooled buffers without allocating per frame
	for {
		_, reader, err := wsConn.Reader(sessCtx)
		if err != nil {
			return
		}

		buf := bufferPool.Get().(*bytes.Buffer)
		buf.Reset()

		if _, err := io.Copy(buf, io.LimitReader(reader, maxFrameSize)); err != nil {
			buf.Reset()
			bufferPool.Put(buf)
			return
		}

		// Non-blocking send: if the client is lagging and the queue is full,
		// drop the frame instead of building multi-second latency.
		select {
		case outbound <- buf:
		default:
			// Optional: never drop your codec header packet (byte 0 == 9)
			if buf.Len() > 0 && buf.Bytes()[0] == 9 {
				// Evict oldest queued frame to make room for the codec header
				select {
				case stale := <-outbound:
					stale.Reset()
					bufferPool.Put(stale)
				default:
				}
				outbound <- buf
			} else {
				buf.Reset()
				bufferPool.Put(buf)
			}
		}
	}
}

func sendPayloadOverUniStream(ctx context.Context, wtSess *webtransport.Session, payload []byte) {
	stream, err := wtSess.OpenUniStreamSync(ctx)
	if err != nil {
		return
	}
	defer stream.Close()

	// Tight deadline ensures a stalled frame aborts quickly instead of blocking the sender loop
	_ = stream.SetWriteDeadline(time.Now().Add(500 * time.Millisecond))
	if _, err := stream.Write(payload); err != nil {
		stream.CancelWrite(0)
	}
}

func forwardUniStreamToWS(ctx context.Context, stream *webtransport.ReceiveStream, wsConn *websocket.Conn) {
	data, err := io.ReadAll(io.LimitReader(stream, maxFrameSize))
	if err != nil {
		log.Printf("error reading client uni-stream: %v", err)
		return
	}
	if len(data) > 0 {
		if err := wsConn.Write(ctx, websocket.MessageBinary, data); err != nil {
			log.Printf("error forwarding to Spring Boot: %v", err)
		}
	}
}
