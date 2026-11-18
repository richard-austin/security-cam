package com.securitycam.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.securitycam.beans.MediaStreamHandler
import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.controllers.RecordingType
import com.securitycam.controllers.Stream
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.mediaserverutils.AVData
import com.securitycam.mediaserverutils.AudioData
import com.securitycam.mediaserverutils.CodecStringDiscovery
import com.securitycam.recording.Recorder
import org.bytedeco.ffmpeg.avcodec.AVCodecDescriptor
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avcodec.AVProfile
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import static org.bytedeco.ffmpeg.global.avcodec.*

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Service
class MediaServer {


    private LogService logService
    private MediaStreamHandler mediaStreamHandler
    private RecorderFactory recorders
    private Config config

    boolean running = false
    private final configFile = "cameras.json"
    ExecutorService mediaThreads

    @Autowired
    MediaServer(private LogService logService, RecorderFactory recorders, MediaStreamHandler mediaStreamHandler, Config config) {
        this.logService = logService
        this.recorders = recorders
        this.mediaStreamHandler = mediaStreamHandler
        this.config = config
    }

    void runStreamingSession(String rtspUrl, String cameraName, Camera cam, String streamName, Stream stream) throws Exception {
        ByteArrayOutputStream aacOutputStream = new ByteArrayOutputStream()
        final boolean hasAudio = stream.audio
        final String rtspTransport = cam.rtspTransport
        AVData avData = null

        mediaThreads.submit(() -> {
            String inputAudioCodecName = ""
            String uri = stream.uri
            int index = uri.indexOf("\u003d")
            if (index == -1)
                index = uri.indexOf("=")
            String camStreamId = ""
            if (index != -1)
                camStreamId = uri.substring(index + "=".length(), uri.length())

            final boolean recordingEnabled = isRecordingEnabled(cam, stream, streamName)

            while (running) {
                // 1. Declare variables here so they are in scope for try, catch, and finally
                FFmpegFrameRecorder recordAAC = null

                avformat.avformat_network_init()
                // Standard out-of-the-box grabber configuration
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspUrl)) {
                    grabber.setOption("rtsp_transport", rtspTransport)

                    // CRUCIAL: Set all 3 native timeouts to protect every layer of the C network handshake (Value is in Microseconds)
                    grabber.setOption("stimeout", "5000000")   // Protects RTSP transport stream negotiation
                    grabber.setOption("timeout", "5000000")    // Protects initial socket connection setup
                    grabber.setOption("rw_timeout", "5000000") // Protects blocking read/write IO calls
                    grabber.setOption("flush_packets", "1")
                    grabber.setOption("fflags", "nobuffer")
                    // 2. Disable decoder frame-reordering delays
                    grabber.setOption("flags", "low_delay")

                    // 3. Prevent multi-threaded decoders from queuing multiple frames internally
                    grabber.setVideoOption("threads", "1")

                    logService.cam.info("Connecting to camera stream...")
                    grabber.start()
                    logService.cam.info("Connected successfully!")

                    avData = getAVData(grabber)
                    mediaStreamHandler.addStream(camStreamId, avData.codecString)

                    inputAudioCodecName = avData.inputAudioCodecName

                    recordAAC = new FFmpegFrameRecorder(aacOutputStream, 0, 0, avData.audioChannels)
                    if (inputAudioCodecName == "aac") {
                        recordAAC.setFormat("adts")
                        recordAAC.setSampleRate(avData.sampleRate)
                        recordAAC.setAudioCodec(avData.inputAudioCodecId)
                        recordAAC.setAudioCodecName("copy")
                        recordAAC.setOption("flush_packets", "1")
                        recordAAC.setOption("fflags", "nobuffer")
                        recordAAC.start(avData.inputFormatContext)
                        AudioData ad = new AudioData(avData)
                        String json = ad.toJSON()
                        mediaStreamHandler.addAudioStream(camStreamId + "a", json)
                    } else if (avData.inputAudioCodecName == "pcm_alaw") {
                        AudioData ad = new AudioData(avData)
                        String json = ad.toJSON()

                        mediaStreamHandler.addAudioStream(camStreamId + "a", json)
                    }
                    // Add recorder if enabled in the config
                    if (recordingEnabled)
                        recorders.addRecorder(cameraName, cam, stream, avData)

                    AVPacket packet
                    logService.cam.info("Routing raw low-latency streams...")

                    // Low-Latency Packet Copying Loop
                    while ((packet = grabber.grabPacket()) != null) {
                        int streamIndex = packet.stream_index()
                        BytePointer data = packet.data()
                        if (data != null && packet.size() > 0) {
                            byte[] buffer = new byte[packet.size()]
                            data.get(buffer)

                            if (hasAudio && streamIndex == avData.audioStreamIndex) {
                                // 1. Clone the packet so recordAAC doesn't alter the original pointers
                                AVPacket clonedPacket = av_packet_clone(packet)
                                if (clonedPacket != null) {
                                    if (avData.audioChannels > 0) {
                                        if (avData.inputAudioCodecName == "aac") {
                                            recordAAC.recordPacket(clonedPacket)
                                            byte[] aacArray = aacOutputStream.toByteArray()
                                            if (aacArray.length > 0)
                                                mediaStreamHandler.sendPacket(camStreamId + "a", aacArray)
                                            aacOutputStream.reset()
                                        } else if (avData.inputAudioCodecName == 'pcm_alaw')
                                            mediaStreamHandler.sendPacket(camStreamId + "a", buffer)
                                        //logService.cam.info("pcm packet len = " + buffer.length)
                                    }

                                    av_packet_free(clonedPacket) // Always free native clones to avoid memory leaks
                                }
                            } else if (streamIndex == avData.videoStreamIndex) {
                                mediaStreamHandler.sendPacket(camStreamId, buffer, (packet.flags() & AV_PKT_FLAG_KEY) != 0)
                            }

                            // Write packet to the recorder if there is one for this stream
                            if (recordingEnabled)
                                recorders.write(packet, cameraName)
                        }
                    }

                    logService.cam.info("Camera closed the stream sequence gracefully.")
                    if (avData.inputAudioCodecName == "aac")
                        recordAAC.stop()

                } catch (Exception e) {
                    logService.cam.error("Stream error occurred: " + e.getMessage())
                    recorders.reset(cameraName, cam, stream, avData)
                    logService.cam.error(e.stackTrace.toString())
                } finally {
                    logService.cam.info("In finally block")

                    if (recordAAC != null && "aac".equals(inputAudioCodecName)) {
                        try {
                            recordAAC.stop()
                        } catch (Exception ignored) {
                        }
                    }
                }

                logService.cam.info("After finally")
                Thread.sleep(2000) // Thread will now safely reach here and retry
            }
        })
    }

    AVData getAVData(FFmpegFrameGrabber grabber) {
        final AVData retVal = new AVData(grabber)

        if (retVal.videoStreamIndex2 != -1) {
            // 1. CRITICAL STEP: Scan the HEVC container headers to properly initialize codecpar()
            // Pass a PointerPointer containing null instead of casting a flat null
            AVCodecParameters codecParams = retVal.inputFormatContext.streams(retVal.videoStreamIndex2).codecpar()

            int codecId = codecParams.codec_id()
            int profile = codecParams.profile() // The profile integer (e.g., 100 for High, 77 for Main)
            int level = codecParams.level()     // The level integer (e.g., 41 for Level 4.1)
            String profileName = "Unknown"

            // Safely look up the name mapping via the codec descriptor
            AVCodecDescriptor descriptor = avcodec_descriptor_get(codecId)
            if (descriptor != null && descriptor.profiles() != null) {
                AVProfile profileArray = descriptor.profiles()
                int i = 0
                while (true) {
                    AVProfile p = profileArray.position(i)
                    int currentProfileId = p.profile()

                    if (currentProfileId == -99 || p.name() == null) {
                        break // End of array marker
                    }

                    if (currentProfileId == profile) {
                        profileName = p.name().getString()
                        break
                    }
                    i++
                }
            }

            // Fetch the short codec name (e.g., "h264", "aac", "hevc")
            BytePointer codecNamePtr = avcodec_get_name(codecId)
            retVal.codecName = (codecNamePtr != null) ? codecNamePtr.getString() : "unknown"
            retVal.codecString = CodecStringDiscovery.constructCodecString(retVal.codecName, profileName, level)
            logService.cam.info("Input Video Profile: " + profile)
            logService.cam.info("Input Video Level: " + level)
            logService.cam.info("video codec " + retVal.inputVideoCodecName)
            logService.cam.info("audio codec: " + retVal.inputAudioCodecName)
        }

        return retVal
    }

    void stop() {
        running = false
        Thread.sleep(2000)
        mediaThreads.shutdown()
    }

    void start() {
        if(!running) {
            try {
                running = true
                mediaThreads = Executors.newFixedThreadPool(40)
                ObjectMapper mapper = new ObjectMapper()
                Map<String, Camera> cameras = mapper.readValue(new File("${config.camerasHomeDirectory}/${configFile}"), Map)
                for (camera in cameras) {
                    Camera c = camera.value as Camera
                    def streams = c.streams
                    logService.cam.info("Camera "+c.name)
                    for (stream in streams) {
                        Stream s = stream.value

                        String netcam_uri = s.netcam_uri
                        if (c.useRtspAuth) {
                            Asymmetric crypto = new Asymmetric()
                            String jsonCreds = crypto.decrypt(c.cred)
                            if (jsonCreds.length() > 0) {
                                def creds = mapper.readValue(jsonCreds, CameraAdminCredentials.class)
                                StringBuilder sb = new StringBuilder(netcam_uri).insert("rtsp://".length(), "${creds.userName}:${creds.password}@")
                                netcam_uri = sb.toString()
                            }

                        }

                        this.runStreamingSession(netcam_uri, camera.key as String, c, stream.key as String, s)
                    }
                }
                logService.cam.info("Config file read " + cameras)
            }
            catch (Exception ex) {
                running = false
                logService.cam.error(ex.getClass().getName() + " in MediaServer.start: " + ex.getMessage())
            }
        }
    }

    boolean isRecordingEnabled(Camera camera, Stream stream, String streamName) {
        boolean enabled = false
        switch(camera.recordingType) {
            case RecordingType.motionService:
                def streams = camera?.streams

                for(s in streams) {
                    if(s.value?.motion?.trigger_recording_on == streamName)
                        enabled = true
                }
                break
            case RecordingType.ftpTriggered:
                enabled = stream?.recording?.enabled
                break
            case RecordingType.pullPointEventTriggered:
                enabled = stream?.recording?.enabled
                break
            default:
                break
        }
        if(enabled)
            logService.cam.info(camera.name+ " $streamName enabled = $enabled")
        return enabled
    }
}
