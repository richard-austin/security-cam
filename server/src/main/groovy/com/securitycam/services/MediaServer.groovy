package com.securitycam.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.securitycam.beans.MediaStreamHandler
import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.mediaserverutils.AVData
import com.securitycam.mediaserverutils.AudioData
import com.securitycam.mediaserverutils.CodecStringDiscovery
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
    private Config config

    boolean running = false
    private final configFile = "cameras.json"
    ExecutorService mediaThreads = Executors.newFixedThreadPool(40)

    @Autowired
    MediaServer(private LogService logService, MediaStreamHandler mediaStreamHandler, Config config) {
        this.logService = logService
        this.mediaStreamHandler = mediaStreamHandler
        this.config = config
    }

    void runStreamingSession(String rtspUrl, String camStreamId, boolean hasAudio, String rtspTransport) throws Exception {
        ByteArrayOutputStream flvOutputStream = new ByteArrayOutputStream()
        ByteArrayOutputStream aacOutputStream = new ByteArrayOutputStream()

        mediaThreads.submit(() -> {
            String inputAudioCodecName = ""

            while (running) {
                // 1. Declare variables here so they are in scope for try, catch, and finally
                FFmpegFrameRecorder recorderFLV = null
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

// 4. Minimize startup probing delay (adjust upward if stream metadata fails to parse)
//                    grabber.setOption("probesize", "32");
//                    grabber.setOption("analyzeduration", "0");


                    logService.cam.info("Connecting to camera stream...")
                    grabber.start()
                    logService.cam.info("Connected successfully!")

                    final AVData avData = getAVData(grabber)
                    mediaStreamHandler.addStream(camStreamId, avData.codecString)

                    inputAudioCodecName = avData.inputAudioCodecName

                    recorderFLV = new FFmpegFrameRecorder(flvOutputStream, avData.width, avData.height, avData.audioChannels)
                    recorderFLV.setFormat("flv")
                    recorderFLV.setVideoCodec(avData.inputVideoCodecId)
                    recorderFLV.setSampleRate(avData.sampleRate)
                    recorderFLV.setAudioCodec(avData.inputAudioCodecId)
                    recorderFLV.setVideoCodecName("copy")
                    recorderFLV.setAudioCodecName("copy")
                    recorderFLV.setOption("flvflags", "no_sequence_end")
                    recorderFLV.setOption("flush_packets", "1")
                    recorderFLV.setOption("fflags", "nobuffer")
                    recorderFLV.start(avData.inputFormatContext)

                    recordAAC = new FFmpegFrameRecorder(aacOutputStream, 0, 0, avData.audioChannels)
                    if (inputAudioCodecName == "aac") {
                        recordAAC.setFormat("adts")
                        recordAAC.setSampleRate(avData.sampleRate)
                        recordAAC.setAudioCodec(avData.inputAudioCodecId)
                        recordAAC.setAudioCodecName("copy")
                        recordAAC.setOption("flush_packets", "1")
                        recorderFLV.setOption("fflags", "nobuffer")
                        recordAAC.start(avData.inputFormatContext)
                        AudioData ad = new AudioData(avData)
                        String json = ad.toJSON()
                        mediaStreamHandler.addAudioStream(camStreamId + "a", json)
                    } else if (avData.inputAudioCodecName == "pcm_alaw") {
                        AudioData ad = new AudioData(avData)
                        String json = ad.toJSON()

                        mediaStreamHandler.addAudioStream(camStreamId + "a", json)
                    }

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
                            recorderFLV.recordPacket(packet)

                            byte[] array = flvOutputStream.toByteArray()

                            if (array.length > 0) {
                                // logService.cam.info("flv output size = " + flvOutputStream.size())
                                flvOutputStream.reset()
                            }
                        }
                    }

                    logService.cam.info("Camera closed the stream sequence gracefully.")
                    recorderFLV.close()
                    recorderFLV.stop()
                    if (avData.inputAudioCodecName == "aac")
                        recordAAC.stop()

                } catch (Exception e) {
                    logService.cam.error("Stream error occurred: " + e.getMessage())
                    logService.cam.error(e.stackTrace.toString())
                } finally {
                    logService.cam.info("In finally block")

                    // Safely close each resource individually with null checks
                    if (recorderFLV != null) {
                        try {
                            recorderFLV.stop()
                        } catch (Exception ignored) {
                        }
                    }
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
    }

    void start() {
        if(!running) {
            try {
                running = true
                ObjectMapper mapper = new ObjectMapper()
                Map<String, Camera> cameras = mapper.readValue(new File("${config.camerasHomeDirectory}/${configFile}"), Map)
                for (camera in cameras) {
                    def c = camera.value
                    def streams = c.streams

                    for (stream in streams) {
                        def s = stream.value
                        String uri = s.uri
                        int index = uri.indexOf("\u003d")
                        if (index == -1)
                            index = uri.indexOf("=")
                        String camStreamId = ""
                        if (index != -1)
                            camStreamId = uri.substring(index + "=".length(), uri.length())

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

                        this.runStreamingSession(netcam_uri, camStreamId, s.audio, c.rtspTransport)
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
}
