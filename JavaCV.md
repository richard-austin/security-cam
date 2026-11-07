
To stream one RTSP input into multiple targets with different codecs (H.264, HEVC, and FLV) using JavaCV, you need to use one FFmpegFrameGrabber and multiple FFmpegFrameRecorder instances simultaneously.
Because decoding and re-encoding video is CPU-heavy, the design splits the raw video/audio frames coming from the single grabber and writes them sequentially into each started recorder.

### Important Protocol & Codec Rules

• FLV Container Constraints: The legacy FLV container natively supports H.264 and AAC. It does not natively support HEVC or G.711 unless you are using specific custom RTMP extensions (like Enhanced RTMP). To maintain absolute stability, your FLV target should pair with H.264 + AAC.
• Timestamp Synchronization: You must copy the grabbed timestamp over to the recorder using recorder.setTimestamp(grabber.getTimestamp()) before calling .record().

```java
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

public class MultiStreamRouter {
    public static void main(String[] args) {
        String rtspInputUrl = "rtsp://YOUR_CAMERA_IP:554/stream1";
        
        // Output Destinations (Can be local files or RTMP/RTSP output URLs)
        String h264Output = "rtmp://localhost/live/h264_aac";
        String hevcOutput = "output_hevc_g711.mp4"; 
        String flvOutput  = "rtmp://localhost/live/flv_stream"; 

        // 1. Initialize Grabber
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspInputUrl)) {
            grabber.setOption("rtsp_transport", "tcp"); // Force TCP for stability
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            double frameRate = grabber.getFrameRate();
            int audioChannels = grabber.getAudioChannels();
            int sampleRate = grabber.getSampleRate();

            // 2. Initialize Recorder 1: H.264 + AAC
            FFmpegFrameRecorder recorderH264 = new FFmpegFrameRecorder(h264Output, width, height, audioChannels);
            recorderH264.setFormat("flv"); // e.g., streaming over RTMP
            recorderH264.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorderH264.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorderH264.setFrameRate(frameRate);
            recorderH264.setSampleRate(sampleRate);
            recorderH264.start();

            // 3. Initialize Recorder 2: HEVC (H.265) + G.711 (PCMA/PCMU)
            FFmpegFrameRecorder recorderHEVC = new FFmpegFrameRecorder(hevcOutput, width, height, audioChannels);
            recorderHEVC.setFormat("mp4"); 
            recorderHEVC.setVideoCodec(avcodec.AV_CODEC_ID_HEVC);
            recorderHEVC.setAudioCodec(avcodec.AV_CODEC_ID_PCM_ALAW); // PCMA (G.711a). Use PCM_MULAW for G.711u
            recorderHEVC.setFrameRate(frameRate);
            recorderHEVC.setSampleRate(sampleRate);
            recorderHEVC.start();

            // 4. Initialize Recorder 3: Standard FLV Stream
            FFmpegFrameRecorder recorderFLV = new FFmpegFrameRecorder(flvOutput, width, height, audioChannels);
            recorderFLV.setFormat("flv");
            recorderFLV.setVideoCodec(avcodec.AV_CODEC_ID_FLV1); // Or AV_CODEC_ID_H264 inside FLV
            recorderFLV.setAudioCodec(avcodec.AV_CODEC_ID_AAC); 
            recorderFLV.setFrameRate(frameRate);
            recorderFLV.setSampleRate(sampleRate);
            recorderFLV.start();

            // 5. Processing Loop
            Frame frame;
            System.out.println("Routing streams started...");
            
            while ((frame = grabber.grab()) != null) {
                long timestamp = grabber.getTimestamp();

                // Broadcast to H264 Recorder
                recorderH264.setTimestamp(timestamp);
                recorderH264.record(frame);

                // Broadcast to HEVC Recorder
                recorderHEVC.setTimestamp(timestamp);
                recorderHEVC.record(frame);

                // Broadcast to FLV Recorder
                recorderFLV.setTimestamp(timestamp);
                recorderFLV.record(frame);
            }

            // 6. Shutdown and Cleanup
            recorderH264.stop();
            recorderHEVC.stop();
            recorderFLV.stop();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Critical Optimization Strategies

1. Audio Re-sampling: G.711 commonly relies on an 8000Hz sample rate, whereas AAC usually runs at 44100Hz or 48000Hz. If your input stream does not match the target codec's required sample rate, you must add an audio re-sampler or explicitly use FFmpegFrameFilter to avoid distorted audio or runtime crashes.
2. Performance Tip: If your input RTSP stream is already encoded in H.264, you can bypass re-encoding for recorderH264 by using Packet-based recording instead of decoding into raw Frame components. Simply grab packets via grabber.grabPacket() and record via recorder.recordPacket(pkt) to save vast amounts of CPU overhead.
   To tailor this better, please tell me:
   • What are the video and audio codecs of your source RTSP stream?
   • Are you streaming to a live media server (like NGINX-RTMP or SRT) or writing to local files?
   AI responses may include mistakes. Learn more
   •
   Home Assistant Community
   Support audio for camera stream - Home Assistant Community
   October 17, 2020 — Transcoding camera audio from unsupported codecs (like G.711) to supported formats (like AAC) is a potential workaround, though it...
   •
   Reddit·r/ffmpeg
   Combine h264 and aac streams into single stream? : r/ffmpeg
   30 Jun 2020 — Combine h264 and aac streams into single stream? I am currently using a solution like so to take a rtsp video stream and create a ...
   •
   Video Production Stack Exchange
   FFmpeg convert G.711 Audio to AAC Audio
   April 7, 2020 — Solution to Audio Problem: The problem was resolved by correcting a minor spelling mistake in the FFmpeg command. now streams audi...
   Show all



I don't want any transcoding, the raw streams should be passed through as is.


To pass the raw streams through without transcoding, you must bypass frame-level decoding entirely and perform packet remuxing. Instead of using grabber.grab() and recorder.record(Frame), you will read raw data chunks via grabber.grabPacket() and pass them directly to recorder.recordPacket(AVPacket).
The answer is to use grabber.grabPacket() and recorder.recordPacket(packet). This completely eliminates CPU decoding/encoding overhead, maintaining 0% extra CPU strain for video compression.

Critical Demuxing/Remuxing Constraints

• Codec Dependency: Because you are passing packets through unchanged, your input RTSP stream must already contain the codec combination you want to output (e.g., the RTSP stream must natively be H.264/AAC or HEVC/G.711). You cannot change codecs without transcoding.
• FLV Container Limitations: The standard FLV/RTMP container natively supports H.264 and AAC. It does not natively support HEVC or G.711 without specialized server-side configurations (Enhanced RTMP). Ensure your input RTSP stream uses H.264 and AAC if targeting standard FLV/RTMP endpoints.

### Packet Passthrough Implementation
```java
import org.bytedeco.ffmpeg.ffmpeg.AVPacket;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

public class RawStreamPassThrough {
    public static void main(String[] args) {
        String rtspInputUrl = "rtsp://YOUR_CAMERA_IP:554/stream1";
        
        // Output targets (Remuxed formats without transcoding)
        String outputTarget1 = "rtmp://localhost/live/h264_aac_stream";
        String outputTarget2 = "output_archive.mp4";

        // 1. Initialize Grabber in Packet Mode
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspInputUrl)) {
            grabber.setOption("rtsp_transport", "tcp"); // Avoid packet drop over UDP
            grabber.start();

            // 2. Initialize Recorder 1 (e.g., RTMP stream)
            FFmpegFrameRecorder recorder1 = new FFmpegFrameRecorder(outputTarget1, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            recorder1.setFormat("flv");
            // Setting the video/audio codec to 0 copies the stream parameters directly without re-encoding
            recorder1.setVideoCodec(0); 
            recorder1.setAudioCodec(0);
            recorder1.setFrameRate(grabber.getFrameRate());
            recorder1.setSampleRate(grabber.getSampleRate());
            recorder1.start(grabber.getFormatContext()); // Pass the original format context to preserve stream headers

            // 3. Initialize Recorder 2 (e.g., MP4 File Dump)
            FFmpegFrameRecorder recorder2 = new FFmpegFrameRecorder(outputTarget2, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            recorder2.setFormat("mp4");
            recorder2.setVideoCodec(0);
            recorder2.setAudioCodec(0);
            recorder2.setFrameRate(grabber.getFrameRate());
            recorder2.setSampleRate(grabber.getSampleRate());
            recorder2.start(grabber.getFormatContext());

            // 4. Packet-level routing loop (0% Transcoding Overhead)
            AVPacket packet;
            System.out.println("Routing raw packets without transcoding...");

            while ((packet = grabber.grabPacket()) != null) {
                // Duplicate and route packets to target 1
                recorder1.recordPacket(packet);
                
                // Duplicate and route packets to target 2
                recorder2.recordPacket(packet);
            }

            // 5. Cleanup
            recorder1.stop();
            recorder2.stop();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```


Key Considerations for Passthrough


Requirement	Behavior in Passthrough Mode
CPU Usage	Extremely Low. It copies raw memory buffers instead of running video matrix math.
start(AVFormatContext)	Mandatory. You must pass grabber.getFormatContext() into recorder.start(...) so the recorder can copy exact SPS/PPS extradata parameters required to parse raw H.264/HEVC frames.
Bitrate Control	Unused. Your output bitrates, resolutions, and frame rates will identically mirror whatever your RTSP camera provides.

AI responses may include mistakes. Learn more

I'm currently able to carry hevc and g711 in flv with the ffmpeg command line.

To replicate your working FFmpeg configuration in JavaCV using raw packet passthrough, you have to map how FFmpeg's tee muxer handles multi-destination stream replication at the packet level.
In JavaCV/FFmpeg, when you call grabber.grabPacket(), the underlying AVPacket maintains an internal index pointing to its source stream (packet.stream_index()). Because you are broadcasting to multiple independent FFmpegFrameRecorder targets (your live stream, audio, and recording endpoints), you must safely duplicate the packet using avcodec.av_packet_clone() or avcodec.av_packet_ref() before passing it down, while matching the expected stream indices for each destination.
Here is the exact JavaCV conversion framework implementing packet passthrough for your scenario:

### JavaCV Packet Passthrough Implementation
````java
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.ArrayList;
import java.util.List;

public class RawStreamTeeRouter {
    public static void main(String[] args) {
        String rtspInputUrl = "rtsp://username:password@YOUR_CAMERA_IP:554/stream";
        
        // Define your targets dynamically mimicking your Go code logic
        String mediaServerInputUri = "rtmp://localhost/live/stream"; // FLV container
        String recordingInputUrl = "output_archive.flv";            // FLV container if configured
        boolean hasAudio = true;
        boolean hasRecording = true;

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspInputUrl)) {
            // Replicating FFmpeg parameters (-timeout 3000000 -rtsp_transport tcp)
            grabber.setOption("rtsp_transport", "tcp"); 
            grabber.setOption("timeout", "3000000");
            grabber.start();

            AVFormatContext inputFormatContext = grabber.getFormatContext();
            
            // Map input streams (Find which stream index is Video vs Audio)
            int inputVideoIndex = -1;
            int inputAudioIndex = -1;
            for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
                AVStream stream = inputFormatContext.streams(i);
                int codecType = stream.codecpar().codec_type();
                if (codecType == org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO && inputVideoIndex == -1) {
                    inputVideoIndex = i;
                } else if (codecType == org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO && inputAudioIndex == -1) {
                    inputAudioIndex = i;
                }
            }

            // Group recorders to dynamically pass packets through via loop
            List<FFmpegFrameRecorder> recorders = new ArrayList<>();

            // 1. Initialize Media Server Output (FLV format supporting HEVC/G711 passthrough)
            FFmpegFrameRecorder liveRecorder = new FFmpegFrameRecorder(mediaServerInputUri, grabber.getImageWidth(), grabber.getImageHeight(), hasAudio ? grabber.getAudioChannels() : 0);
            liveRecorder.setFormat("flv");
            liveRecorder.setVideoCodec(0); // 0 explicitly sets copy/passthrough mode
            if (hasAudio) liveRecorder.setAudioCodec(0);
            
            // Mimicking FFmpeg flags: -fflags nobuffer+flush_packets -> maps to avioflags/fflags options
            liveRecorder.setOption("fflags", "nobuffer+flush_packets");
            liveRecorder.setOption("avioflags", "direct");
            
            // Critical step: Pass down input Format Context so muxer inherits original stream configuration headers
            liveRecorder.start(inputFormatContext);
            recorders.add(liveRecorder);

            // 2. Initialize Recording Dump if enabled
            FFmpegFrameRecorder archiveRecorder = null;
            if (hasRecording) {
                archiveRecorder = new FFmpegFrameRecorder(recordingInputUrl, grabber.getImageWidth(), grabber.getImageHeight(), hasAudio ? grabber.getAudioChannels() : 0);
                archiveRecorder.setFormat("flv");
                archiveRecorder.setVideoCodec(0);
                if (hasAudio) archiveRecorder.setAudioCodec(0);
                archiveRecorder.start(inputFormatContext);
                recorders.add(archiveRecorder);
            }

            System.out.println("Tee passthrough active. Intercepting and routing packets...");

            AVPacket packet;
            // Loop captures raw data units (AVPackets) straight from the demuxer layer
            while ((packet = grabber.grabPacket()) != null) {
                int currentStreamIndex = packet.stream_index();

                // Handle conditional packet dropping (if audio flag is explicitly turned off)
                if (currentStreamIndex == inputAudioIndex && !hasAudio) {
                    continue;
                }

                // Push identical packet structure out to every registered target
                for (FFmpegFrameRecorder recorder : recorders) {
                    // Create an independent memory reference clone of the underlying packet 
                    // preventing data contamination/race conditions across internal multi-muxer flushes.
                    AVPacket clonedPacket = avcodec.av_packet_clone(packet);
                    if (clonedPacket != null) {
                        // Remuxing requires mapping track positions relative to output format profiles
                        clonedPacket.stream_index(currentStreamIndex);
                        
                        // Output the packet directly to the streaming target container
                        recorder.recordPacket(clonedPacket);
                        
                        // Free native structural handles immediately to avoid serious JVM heap memory leaks
                        avcodec.av_packet_free(clonedPacket);
                    }
                }
            }

            // Tear down targets sequentially
            for (FFmpegFrameRecorder recorder : recorders) {
                recorder.stop();
                recorder.release();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
````


Essential Architectural Equivalences

1. setVideoCodec(0) and setAudioCodec(0): This is the identical operational mapping to -c:v copy and -c:a copy. It tells FFmpeg's codec descriptor pipeline to instantiate direct memory copies instead of looking up operational transcoder matrix algorithms (like libx264 or libfaac).
2. av_packet_clone(packet): When using raw packet passthrough across multiple destinations in JavaCV, you cannot pipe the exact same AVPacket reference directly into separate recorders repeatedly. The internal pointer arrays mutate when passing through different container logic formats. Cloning handles explicit buffer mirroring smoothly.
3. Handling Custom FLV Format Tags: Because standard configurations of classical FLV containers choke on modern payloads like HEVC, your underlying media infrastructure (the target paths specified) must already implement the Enhanced RTMP extensions. Since your custom command line functions smoothly, JavaCV will run cleanly out of the box because it links natively back to the same underlying shared C library configurations (libavformat/libavcodec) present on your target deployment environment.
   To ensure the multi-track layout passes flawlessly, let me know:
   • Does your RTSP camera output multiple separate audio tracks or only a single combined stream source?
   • Do you experience any intermittent packet dropping or timestamp synchronization errors when executing your current script sequence?
   AI responses may include mistakes. Learn more

Is it possible not to use the tee muxer with javacv for this? Using tee to produce, say an hevc, g711 and flv stream puts some latency on the audio which is not there without the flv stream incluuded.

Yes, it is absolutely possible to avoid the tee muxer completely, and doing so is highly recommended if you are seeing latency.
When you use the tee muxer in an absolute passthrough environment, all output streams are synchronized and bound by the same internal queue and blocking network writes. If your FLV server endpoint introduces slight network congestion or delays a packet acknowledgment, the tee muxer stalls all outputs to keep them aligned, which immediately manifests as latency or drift on your audio track.
By managing the distribution explicitly in JavaCV packet loop using multiple independent FFmpegFrameRecorder instances, each target gets its own detached output buffer and connection. A delay on the FLV stream will no longer choke or lag the HEVC/G.711 stream.

Pure JavaCV Multi-Output (No tee) Implementation

The following implementation connects to your RTSP source, reads raw packets, and handles distribution to multiple distinct destinations entirely in user-space memory:

````java
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.ArrayList;
import java.util.List;

public class DecoupledPacketRouter {
    public static void main(String[] args) {
        String rtspInputUrl = "rtsp://username:password@YOUR_CAMERA_IP:554/stream";
        
        // Define isolated endpoints
        String hevcG711StreamUrl = "rtsp://localhost:8554/hevc_g711"; // Separate clean target
        String flvStreamUrl = "rtmp://localhost/live/flv_legacy";     // Flv/RTMP target causing latency
        String localRecordingPath = "archive.mp4";

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspInputUrl)) {
            grabber.setOption("rtsp_transport", "tcp"); 
            grabber.setOption("timeout", "3000000");
            grabber.start();

            AVFormatContext inputFormatContext = grabber.getFormatContext();
            
            // Map tracks to confirm stream existence
            int videoIdx = -1, audioIdx = -1;
            for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
                AVStream s = inputFormatContext.streams(i);
                if (s.codecpar().codec_type() == avutil.AVMEDIA_TYPE_VIDEO && videoIdx == -1) videoIdx = i;
                if (s.codecpar().codec_type() == avutil.AVMEDIA_TYPE_AUDIO && audioIdx == -1) audioIdx = i;
            }

            List<FFmpegFrameRecorder> recorders = new ArrayList<>();

            // Output 1: HEVC + G.711 stream
            FFmpegFrameRecorder hevcRecorder = new FFmpegFrameRecorder(hevcG711StreamUrl, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            hevcRecorder.setFormat("rtsp"); // or "mpegts", "mp4" depending on destination
            hevcRecorder.setVideoCodec(0);  // 0 = passthrough / copy
            hevcRecorder.setAudioCodec(0);
            hevcRecorder.setOption("fflags", "nobuffer+flush_packets");
            hevcRecorder.setOption("avioflags", "direct");
            hevcRecorder.start(inputFormatContext);
            recorders.add(hevcRecorder);

            // Output 2: Isolated FLV Stream (Will not block Output 1 if network latency occurs)
            FFmpegFrameRecorder flvRecorder = new FFmpegFrameRecorder(flvStreamUrl, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            flvRecorder.setFormat("flv");
            flvRecorder.setVideoCodec(0);
            flvRecorder.setAudioCodec(0);
            // Low latency tuning specifically for the FLV pipeline
            flvRecorder.setOption("fflags", "nobuffer+flush_packets");
            flvRecorder.setOption("avioflags", "direct");
            flvRecorder.start(inputFormatContext);
            recorders.add(flvRecorder);

            // Output 3: Local File Dump
            FFmpegFrameRecorder fileRecorder = new FFmpegFrameRecorder(localRecordingPath, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            fileRecorder.setFormat("mp4");
            fileRecorder.setVideoCodec(0);
            fileRecorder.setAudioCodec(0);
            fileRecorder.start(inputFormatContext);
            recorders.add(fileRecorder);

            System.out.println("Routing packets via isolated loops...");

            AVPacket packet;
            while ((packet = grabber.grabPacket()) != null) {
                
                for (FFmpegFrameRecorder recorder : recorders) {
                    // Deep copy the packet so mutations inside a recorder's muxer 
                    // do not impact the timestamps/buffers of parallel streams
                    AVPacket clonedPacket = avcodec.av_packet_clone(packet);
                    if (clonedPacket != null) {
                        
                        // Pass packet directly to the underlying isolated muxer
                        recorder.recordPacket(clonedPacket);
                        
                        // Prevent native pointer memory leaks
                        avcodec.av_packet_free(clonedPacket);
                    }
                }
            }

            // Close targets
            for (FFmpegFrameRecorder recorder : recorders) {
                recorder.stop();
                recorder.release();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
````


Why This Fixes Audio Latency

1. Decoupled Network Blocking: In your original Go code calling FFmpeg CLI, the -f tee muxer acts as a synchronous master. If the stream writing to stream.MediaServerInputUri blocks on a network socket write for even 40 milliseconds, the whole loop halts. This causes the audio packets intended for the other destinations to buffer up or drop, causing the lag. With JavaCV recorders, each recorder.recordPacket() operates independently on its own underlying network socket descriptor.
2. Dynamic Thread Offloading (Optional Step): If the network latency to your FLV endpoint is severe, writing sequentially in a single thread can still create slight delays. If latency persists with the code above, you can pass the cloned packets into a Java LinkedBlockingQueue for each recorder, and let a small thread pool call recorder.recordPacket() asynchronously. This completely insulates your HEVC stream from anything happening on the FLV network route.
   If you want to take performance a step further, let me know:
   • Is your application running on a resource-constrained server, or do you have enough overhead for asynchronous threading?
   • Are your target streaming endpoints located on the same local network or across the public internet?


I will be using this to feed a web transport endpoint for the lowest possible latency AV streams, but I want to do some tests with javacv first.



Using JavaCV to feed a WebTransport endpoint for ultra-low latency is a fantastic design choice. WebTransport runs over HTTP/3 (QUIC), which uses unreliable datagrams or independent streams. This means you completely avoid head-of-line blocking—a massive advantage over TCP-based RTMP or RTSP.
However, testing this with JavaCV requires a few crucial modifications to your packet routing loop. When you bypass the tee muxer and route packets directly to real-time protocols, the native FFmpeg muxer layer inside FFmpegFrameRecorder will modify packet timestamps (DTS/PTS) during interleaved blocking writes.
If you sequentially pass the same packet or a simple clone into multiple recorders in a single-threaded loop, the second and third recorders will receive timestamps skewed by the network latency of the first recorder. This directly re-introduces the audio drift and latency you are trying to eliminate.
To achieve true, unblocked real-time delivery for your upcoming WebTransport architecture, you must decouple the recorders into an Asynchronous Packet Fan-out model using standard Java concurrent queues.

Thread-Isolated Packet Router Implementation

This architecture uses a single thread to pull raw packets from the RTSP camera as fast as possible and immediately drops them into memory-backed queues. Each output target runs on its own background thread, completely insulating your low-latency streams from any FLV endpoint bottlenecks.

````java
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UltraLowLatencyRouter {

    private static final AtomicBoolean running = new AtomicBoolean(true);

    // Thread-safe worker to handle isolated muxing and streaming
    static class RecorderWorker implements Runnable {
        private final String targetUrl;
        private final String format;
        private final AVFormatContext grabberContext;
        private final int width, height, channels;
        private final BlockingQueue<AVPacket> packetQueue = new LinkedBlockingQueue<>(100); // Caps memory buffer

        public RecorderWorker(String targetUrl, String format, FFmpegFrameGrabber grabber) {
            this.targetUrl = targetUrl;
            this.format = format;
            this.grabberContext = grabber.getFormatContext();
            this.width = grabber.getImageWidth();
            this.height = grabber.getImageHeight();
            this.channels = grabber.getAudioChannels();
        }

        public void offerPacket(AVPacket packet) {
            // Clone packet inside the ingestion thread to maintain thread-confined memory bounds
            AVPacket cloned = avcodec.av_packet_clone(packet);
            if (cloned != null) {
                // If queue fills up (e.g., FLV server stalls), drop old packets to maintain ultra-low latency
                if (!packetQueue.offer(cloned)) {
                    avcodec.av_packet_free(cloned); // Instantly free dropped frame pointers
                }
            }
        }

        @Override
        public void run() {
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(targetUrl, width, height, channels)) {
                recorder.setFormat(format);
                recorder.setVideoCodec(0); // Pure pass-through
                recorder.setAudioCodec(0);
                
                // Zero-latency overrides for the internal FFmpeg engine
                recorder.setOption("fflags", "nobuffer+flush_packets");
                recorder.setOption("avioflags", "direct");
                recorder.setOption("tune", "zerolatency");
                
                recorder.start(grabberContext);

                while (running.get() || !packetQueue.isEmpty()) {
                    AVPacket pkt = packetQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (pkt != null) {
                        recorder.recordPacket(pkt);
                        avcodec.av_packet_free(pkt); // Prevent off-heap memory leaks
                    }
                }
                recorder.stop();
            } catch (Exception e) {
                System.err.println("Error on worker target [" + targetUrl + "]: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        String rtspInputUrl = "rtsp://username:password@YOUR_CAMERA_IP:554/stream";

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspInputUrl)) {
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setOption("timeout", "3000000");
            grabber.start();

            // Instantiate independent workers
            RecorderWorker hevcWorker = new RecorderWorker("rtsp://localhost:8554/hevc_low_latency", "rtsp", grabber);
            RecorderWorker flvWorker = new RecorderWorker("rtmp://localhost/live/flv_legacy", "flv", grabber);

            Thread t1 = new Thread(hevcWorker, "HEVC-Stream-Thread");
            Thread t2 = new Thread(flvWorker, "FLV-Stream-Thread");
            t1.start();
            t2.start();

            System.out.println("Asynchronous Real-Time Demuxer Active...");

            AVPacket packet;
            while (running.get() && (packet = grabber.grabPacket()) != null) {
                // Instantly hand off packets to workers without waiting for network IO
                hevcWorker.offerPacket(packet);
                flvWorker.offerPacket(packet);
            }

            running.set(false);
            t1.join();
            t2.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
````


Strategic Testing Adjustments for WebTransport Preparation

When staging tests before shifting completely over to WebTransport, monitor and fine-tune these parameters:
1. Queue Capacity Tuning: The LinkedBlockingQueue size is set to 100. In a 30fps stream, this is roughly 3 seconds of buffer. If you want absolute, cutthroat low-latency, reduce this boundary down to 5 or 10. This forces the application to aggressively drop frames if the target pipeline lags, preserving real-time delivery on the other threads.
2. Memory Leak Auditing: Because packet passing happens natively via JNI wrappers, failing to call av_packet_free() inside the worker loop will break the JVM garbage collector boundary. Ensure the packet freeing logic is kept close to where the packet is popped from the queue.
3. Audio Sampling Mismatches: Since WebTransport setups typically expect standardized PCM, G.711, or Opus over raw UDP/QUIC datagrams, passing a raw RTSP feed directly through can occasionally flag codec descriptor issues depending on how strict your target endpoint ingestion engine is.
   As you move your validation tests forward, let me know:
   • How are you handling packet reassembly and parsing on the receiving side of your WebTransport endpoint?
   • Do you plan to extract raw annex-B byte arrays (NAL units) from the JavaCV packets to pass to the WebTransport socket?
   AI responses may include mistakes. Learn more

The flv stream is read by a recording service to record video when certain triggers occur. It couls be better if rather than produce an flv stream, that it could be treiggered to produce hls files on demand (they would typically be about 30s - 1 minute long).

Converting your real-time processing loop to generate short, on-demand HLS (HTTP Live Streaming) video fragments instead of a constant FLV push is a highly efficient structural optimization.
Instead of keeping an active, continuous network socket open to a legacy FLV media server, your JavaCV loop can conditionally instantiate an FFmpegFrameRecorder targeted at a local directory or memory-backed file system whenever a trigger event happens. This keeps your core ingestion pipeline unburdened and zero-latency, while generating industry-standard .m3u8 and .ts files ready for instant playback or archiving.

Architectural Strategy for On-Demand HLS

Because you are doing zero-transcoding packet passthrough, you cannot use classical time-based slicing commands blindly. HLS fragments must split exactly on a Video Keyframe (I-frame / IDR). If you start or stop an HLS recorder on a B-frame or P-frame, the video player will crash or experience severe green-screen artifacts until the next keyframe arrives.
To achieve clean on-demand cuts:
1. Detect Keyframes: Intercept each packet and check if its keyframe flag is set (packet.flags() & avcodec.AV_PKT_FLAG_KEY).
2. State-Driven Recording: Maintain a volatile boolean state (isRecording). When a trigger occurs, turn this flag to true.
3. Graceful Activation/Deactivation: Only start the HLS recorder when isRecording == true and a keyframe arrives. Similarly, when the 30-60 second timer expires, set a teardown flag and wait for the next keyframe before calling recorder.stop() to ensure a clean visual termination.

On-Demand HLS Segmenter Implementation


```java
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class TriggeredHlsRecorder {

    // Thread-safe triggers controlled by your external application logic
    private static final AtomicBoolean triggerActive = new AtomicBoolean(false);
    private static long recordStopTime = 0;

    public static void main(String[] args) {
        String rtspInputUrl = "rtsp://username:password@YOUR_CAMERA_IP:554/stream";
        String hlsOutputDir = "/tmp/hls_clips/"; // Or an NVMe/RAM disk for maximum performance
        
        // Ensure output folder exists
        new File(hlsOutputDir).mkdirs();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspInputUrl)) {
            grabber.setOption("rtsp_transport", "tcp");
            grabber.start();

            AVFormatContext grabberContext = grabber.getFormatContext();
            FFmpegFrameRecorder hlsRecorder = null;
            boolean isMuxerRunning = false;

            System.out.println("Low-latency stream engine running. Awaiting triggers...");

            // Simulated external trigger thread (for testing demonstration)
            simulateExternalTriggers();

            AVPacket packet;
            while ((packet = grabber.grabPacket()) != null) {
                boolean isKeyframe = (packet.flags() & avcodec.AV_PKT_FLAG_KEY) != 0;
                long currentTime = System.currentTimeMillis();

                // State A: Trigger active, but recorder not yet spun up
                if (triggerActive.get() && !isMuxerRunning) {
                    // CRITICAL: Only start recording on a true Keyframe boundary
                    if (isKeyframe) {
                        String clipId = "clip_" + (currentTime / 1000);
                        String playlistPath = hlsOutputDir + clipId + ".m3u8";
                        
                        hlsRecorder = new FFmpegFrameRecorder(playlistPath, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
                        hlsRecorder.setFormat("hls");
                        hlsRecorder.setVideoCodec(0); // Passthrough H.264 / HEVC
                        hlsRecorder.setAudioCodec(0); // Passthrough AAC / G.711
                        
                        // HLS Muxer Custom Optimization Options
                        hlsRecorder.setOption("hls_time", "4");           // Target duration for internal TS segments (seconds)
                        hlsRecorder.setOption("hls_list_size", "0");       // 0 keeps all segments in the playlist (creates a complete VOD clip)
                        hlsRecorder.setOption("hls_segment_type", "mpegts"); // Use standard TS segments
                        
                        hlsRecorder.start(grabberContext);
                        isMuxerRunning = true;
                        System.out.println(">>> HLS On-Demand Recording Started: " + playlistPath);
                    }
                }

                // State B: Actively routing packets to the HLS muxer
                if (isMuxerRunning && hlsRecorder != null) {
                    AVPacket cloned = avcodec.av_packet_clone(packet);
                    if (cloned != null) {
                        hlsRecorder.recordPacket(cloned);
                        avcodec.av_packet_free(cloned);
                    }

                    // Check if recording duration has elapsed
                    if (currentTime >= recordStopTime) {
                        // CRITICAL: Safely exit on a keyframe to prevent corrupted trailing segments
                        if (isKeyframe) {
                            triggerActive.set(false);
                            isMuxerRunning = false;
                            hlsRecorder.stop();
                            hlsRecorder.release();
                            hlsRecorder = null;
                            System.out.println("<<< HLS On-Demand Recording Complete cleanly.");
                        }
                    }
                }

                // Push to WebTransport pipeline goes here (completely unaffected by the HLS logic)
                // webTransportPipeline.push(packet); 
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper to simulate an external trigger arriving 10 seconds into execution
    private static void simulateExternalTriggers() {
        new Thread(() -> {
            try {
                Thread.sleep(10000); // Wait 10 seconds
                System.out.println("[TRIGGER] Motion or system event detected!");
                
                // Configure duration bounds (e.g., record for 45 seconds)
                recordStopTime = System.currentTimeMillis() + 45000;
                triggerActive.set(true);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```


Strategic Tuning Parameters for HLS Passthrough

• hls_time vs Camera GOP Size: Since you are passing raw packets through without re-encoding, FFmpeg cannot force a segment split exactly at your chosen hls_time value if the camera isn't generating keyframes there. If your camera has a Keyframe Interval (GOP) of 2 seconds, set hls_time to a multiple of that (e.g., 2, 4, or 6) for precise segment sizing.
• hls_list_size 0: By default, HLS is tuned for live streaming and deletes older segments from the index file. Setting this parameter to 0 tells the muxer to append every segment sequentially to the .m3u8 index, turning the output into a pristine, stand-alone VOD (Video On Demand) clip file ready for standard browser playback when completed.
• RAM Disk Writing (/dev/shm or tmpfs): To avoid disk I/O bottlenecks causing micro-stuttering on your low-latency WebTransport thread, point your hlsOutputDir to a memory-backed path if running on Linux. Your recording service can fetch the complete folder once the loop signals the file closing is finished.
To optimize the HLS asset storage footprint, let me know:
• Where does your recording service save the files permanently—local storage, network attached storage (NAS), or cloud object storage?
• How many simultaneous camera streams are you planning to scale this architecture to on a single server?
AI responses may include mistakes. Learn more

Does this handle the raw AV streams too?

Yes, this handles the raw AV streams perfectly without any transcoding.
Because the lines hlsRecorder.setVideoCodec(0) and hlsRecorder.setAudioCodec(0) are explicitly set, JavaCV bypasses the encoder layer entirely. It takes the exact raw compressed packets (H.264, HEVC, AAC, or G.711) grabbed from your RTSP source and packages them directly into standard MPEG-TS (.ts) container chunks.

Why this works with your specific codecs

The MPEG-TS container used by HLS natively supports almost all combinations of your raw streams out of the box:
• H.264 + AAC: Fully compliant, standard HLS profile.
• HEVC (H.265) + AAC: Native to Apple devices and modern web players via fMP4 or modern TS extensions.
• H.264/HEVC + G.711 (PCMA/PCMU): This will create valid .ts files, but note that standard web browsers (like Chrome or Safari) cannot natively play back G.711 audio via an HLS player directly. If your recording storage system or playback app uses an FFmpeg-backed media engine (like VLC or a custom video architecture), it will read the passthrough audio without issue.

A Small Warning on Keyframe Intervals (GOP)

Because you are copying the packets directly (codec 0), JavaCV cannot force a new keyframe to happen when the recording trigger fires. It must wait a split second for the camera's natural internal timeline to emit an I-Frame before the HLS file initializes. This guarantees your recorded files open cleanly without green screens or blocky distortion.
Depending on your playback setup, let me know:
• Will these recorded files be played back in a standard web browser (which requires AAC) or through a dedicated security app/VLC player (which can handle raw G.711)?
AI responses may include mistakes. Learn more

The current system uses a gop cache to ensure you can always start from a keyframe

Since your system already maintains a GOP (Group of Pictures) cache, you can instantly write historical packets to the on-demand HLS recorder the moment a trigger fires. This eliminates the delay of waiting for the next camera keyframe, ensuring your 30–60 second clips include the critical moments right before or exactly at the trigger time.
When implementing a GOP cache with FFmpegFrameRecorder in passthrough mode (setVideoCodec(0)), you must handle DTS/PTS timestamp discontinuities and memory ownership carefully.

How to Pipe a GOP Cache into On-Demand HLS Passthrough

Because you are passing raw packets, the HLS muxer needs monotonically increasing timestamps starting near zero for the playlist to be valid. When you flush your cache, you should calculate a timestamp offset based on the first keyframe in your cache.
Here is how to adapt your packet loop to flush an existing GOP cache into the HLS recorder:

```java
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class GopCachedHlsRecorder {

    private static final AtomicBoolean triggerActive = new AtomicBoolean(false);
    private static long recordStopTime = 0;

    // A simple bounded look-back cache for raw packets
    private static final LinkedList<AVPacket> gopCache = new LinkedList<>();
    private static final int MAX_CACHE_PACKETS = 150; // Adjust based on camera frame rate / GOP length

    public static void main(String[] args) {
        String rtspInputUrl = "rtsp://username:password@YOUR_CAMERA_IP:554/stream";
        String hlsOutputDir = "/tmp/hls_clips/";
        new File(hlsOutputDir).mkdirs();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspInputUrl)) {
            grabber.setOption("rtsp_transport", "tcp");
            grabber.start();

            AVFormatContext grabberContext = grabber.getFormatContext();
            FFmpegFrameRecorder hlsRecorder = null;
            boolean isMuxerRunning = false;
            
            // Offset tracking to fix HLS timeline when dumping history
            long ptsOffset = 0;
            long dtsOffset = 0;

            simulateExternalTriggerAfterDelay();

            AVPacket packet;
            while ((packet = grabber.grabPacket()) != null) {
                boolean isKeyframe = (packet.flags() & avcodec.AV_PKT_FLAG_KEY) != 0;
                long currentTime = System.currentTimeMillis();

                // 1. Maintain the GOP Cache
                // If we hit a new keyframe and aren't recording, we can clear old GOPs
                if (isKeyframe && !isMuxerRunning) {
                    clearGopCache(); 
                }
                
                // Clone and store packet in the historical cache
                AVPacket cacheClone = avcodec.av_packet_clone(packet);
                if (cacheClone != null) {
                    gopCache.addLast(cacheClone);
                    if (gopCache.size() > MAX_CACHE_PACKETS) {
                        avcodec.av_packet_free(gopCache.removeFirst());
                    }
                }

                // 2. Handle Trigger Activation via GOP Cache Flush
                if (triggerActive.get() && !isMuxerRunning) {
                    String clipId = "clip_" + (currentTime / 1000);
                    String playlistPath = hlsOutputDir + clipId + ".m3u8";

                    hlsRecorder = new FFmpegFrameRecorder(playlistPath, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
                    hlsRecorder.setFormat("hls");
                    hlsRecorder.setVideoCodec(0); // Passthrough
                    hlsRecorder.setAudioCodec(0);
                    hlsRecorder.setOption("hls_time", "4");
                    hlsRecorder.setOption("hls_list_size", "0");
                    hlsRecorder.setOption("hls_segment_type", "mpegts");
                    
                    hlsRecorder.start(grabberContext);
                    isMuxerRunning = true;
                    System.out.println(">>> Trigger tripped! Flushing GOP cache to HLS: " + playlistPath);

                    // Determine time offsets from the very first packet in our cache
                    if (!gopCache.isEmpty()) {
                        ptsOffset = gopCache.getFirst().pts();
                        dtsOffset = gopCache.getFirst().dts();
                    }

                    // Flush cache into the recorder
                    while (!gopCache.isEmpty()) {
                        AVPacket cachedPkt = gopCache.removeFirst();
                        
                        // Normalize timestamps relative to the start of this clip
                        cachedPkt.pts(cachedPkt.pts() - ptsOffset);
                        cachedPkt.dts(cachedPkt.dts() - dtsOffset);

                        hlsRecorder.recordPacket(cachedPkt);
                        avcodec.av_packet_free(cachedPkt); // Free after writing
                    }
                }

                // 3. Active Recording Phase (Continuous Passthrough)
                if (isMuxerRunning && hlsRecorder != null) {
                    AVPacket liveClone = avcodec.av_packet_clone(packet);
                    if (liveClone != null) {
                        // Keep applying the same historical offset to the incoming real-time packets
                        liveClone.pts(liveClone.pts() - ptsOffset);
                        liveClone.dts(liveClone.dts() - dtsOffset);

                        hlsRecorder.recordPacket(liveClone);
                        avcodec.av_packet_free(liveClone);
                    }

                    // Stop recording when duration elapses (Clean termination on keyframe)
                    if (currentTime >= recordStopTime && isKeyframe) {
                        triggerActive.set(false);
                        isMuxerRunning = false;
                        hlsRecorder.stop();
                        hlsRecorder.release();
                        hlsRecorder = null;
                        clearGopCache();
                        System.out.println("<<< HLS Clip finished cleanly.");
                    }
                }
                
                // Your independent ultra-low latency WebTransport push happens here 
                // Using the pristine un-offset raw 'packet'
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearGopCache() {
        while (!gopCache.isEmpty()) {
            avcodec.av_packet_free(gopCache.removeFirst());
        }
    }

    private static void simulateExternalTriggerAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(8000); 
                System.out.println("[TRIGGER] Event registered!");
                recordStopTime = System.currentTimeMillis() + 30000; // 30 second clip
                triggerActive.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```


Critical Rules for GOP Passthrough in JavaCV

1. Timestamp Normalization: RTSP streams have large, arbitrary timestamps based on when the camera booted up. When you pull packets out of your cache and dump them into a brand-new HLS container, you must subtracting the initial PTS/DTS offsets (as shown above). If you don't, the first segment of your HLS file might declare it starts at Timestamp: 482942.02, which breaks web players.
2. Double-Free Prevention: Notice that the cache mechanism explicitly clones the packets via av_packet_clone(packet). Because grabber.grabPacket() manages its own internal recycle buffer, you must duplicate the packet to store it safely in your memory array. Always free it using av_packet_free() when it leaves the cache or after it's recorded.
3. WebTransport Separation: Because you mutate packet properties (pts and dts) when mapping them to the HLS recorder timeline, make sure you perform your WebTransport streaming pushes using the original unmodified packet from the grabber loop, keeping your live pipeline clean and unskewed.
   To ensure perfect cross-platform playback of these cached outputs, tell me:
   • Do your cameras output constant frame rates (CFR) or variable frame rates (VFR)?
   • Do you intend to read the completed HLS files via a custom Go/Java service, or directly using a web front-end (HLS.js / Video.js)?
