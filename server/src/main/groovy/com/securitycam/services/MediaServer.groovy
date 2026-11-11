package com.securitycam.services

import com.securitycam.beans.MediaStreamHandler
import com.securitycam.mediaserverutils.AVData
import com.securitycam.mediaserverutils.AudioData
import com.securitycam.mediaserverutils.CodecStringDiscovery
import org.bytedeco.ffmpeg.avcodec.AVCodecDescriptor
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avcodec.AVProfile
import org.bytedeco.ffmpeg.global.avutil
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
    @Autowired
    private LogService logService

    ExecutorService mediaThreads = Executors.newFixedThreadPool(40)

    @Autowired
    private MediaStreamHandler mediaStreamHandler

    void runStreamingSession(String rtspUrl) throws Exception {
        ByteArrayOutputStream flvOutputStream = new ByteArrayOutputStream()
        ByteArrayOutputStream aacOutputStream = new ByteArrayOutputStream()

        mediaThreads.submit(() -> {
            String inputAudioCodecName = ""

            while (true) {
                // 1. Declare variables here so they are in scope for try, catch, and finally
                FFmpegFrameRecorder recorderFLV = null
                FFmpegFrameRecorder recordAAC = null

                avformat.avformat_network_init()
                // Standard out-of-the-box grabber configuration
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspUrl)) {
                    grabber.setOption("rtsp_transport", "tcp")

                    // CRUCIAL: Set all 3 native timeouts to protect every layer of the C network handshake (Value is in Microseconds)
                    grabber.setOption("stimeout", "5000000")   // Protects RTSP transport stream negotiation
                    grabber.setOption("timeout", "5000000")    // Protects initial socket connection setup
                    grabber.setOption("rw_timeout", "5000000") // Protects blocking read/write IO calls

                    logService.cam.info("Connecting to camera stream...")
                    // start() is now entirely safe if the camera is dead, the C layer will abort in 5 seconds
                    grabber.start()
                    logService.cam.info("Connected successfully!")

                    final AVData avData = getAVData(grabber)
                    mediaStreamHandler.addStream("cam4-stream1", avData.codecString)

                    inputAudioCodecName = avData.inputAudioCodecName

                    recorderFLV = new FFmpegFrameRecorder(flvOutputStream, avData.width, avData.height, avData.audioChannels)
                    recorderFLV.setFormat("flv")
                    recorderFLV.setVideoCodec(avData.inputVideoCodecId)
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
                        recordAAC.setAudioCodec(avData.inputAudioCodecId)
                        recordAAC.setAudioCodecName("copy")
                        recordAAC.setOption("flush_packets", "1")
                        recorderFLV.setOption("fflags", "nobuffer")
                        recordAAC.start(avData.inputFormatContext)
                    } else if (avData.inputAudioCodecName == "pcm_alaw") {
                        AudioData ad = new AudioData(avData)
                        String json = ad.toTextMessage()

                        mediaStreamHandler.addAudioStream("cam4-stream1a", json)
                    }

                    AVPacket packet
                    logService.cam.info("Routing raw low-latency streams...")

                    // Track the last recorded DTS for each stream index to guarantee monotonic growth
                    Map<Integer, Long> lastDtsMap = new HashMap<>()
                    // Low-Latency Packet Copying Loop
                    while ((packet = grabber.grabPacket()) != null) {
                        int streamIndex = packet.stream_index()

                        // 1. FIX TIMESTAMPS FIRST (Before any recording/writing happens)
                        if (packet.pts() == avutil.AV_NOPTS_VALUE || packet.pts() < 0) {
                            long currentGrabberTimestamp = grabber.getTimestamp() // Microseconds
                            packet.pts(currentGrabberTimestamp)
                            packet.dts(currentGrabberTimestamp)
                        }

                        // --- NEW: MONOTONIC DTS ENFORCEMENT ---
                        long currentDts = packet.dts()
                        if (lastDtsMap.containsKey(streamIndex)) {
                            long lastDts = lastDtsMap.get(streamIndex)
                            if (currentDts <= lastDts) {
                                // Force the timestamp forward by exactly 1 unit to satisfy the muxer
                                currentDts = lastDts + 1
                                packet.dts(currentDts)

                                // Adjust PTS to maintain the original delta relationship if it falls behind
                                if (packet.pts() < currentDts) {
                                    packet.pts(currentDts)
                                }
                            }
                        }
                        // Update our tracker with the final chosen timestamp
                        lastDtsMap.put(streamIndex, currentDts)
                        // --------------------------------------

                        // ... Rest of your original byte buffer extraction and routing code ...
                        // 1. FIX TIMESTAMPS FIRST (Before any recording/writing happens)
                        if (packet.pts() == avutil.AV_NOPTS_VALUE || packet.pts() < 0) {
                            long currentGrabberTimestamp = grabber.getTimestamp() // Microseconds
                            packet.pts(currentGrabberTimestamp)
                            packet.dts(currentGrabberTimestamp)
                        }

                        // Cache original values for safety
                        long origPts = packet.pts()
                        long origDts = packet.dts()
                        long origDuration = packet.duration()
                        int origStreamIndex = packet.stream_index()

                        BytePointer data = packet.data()
                        if (data != null && packet.size() > 0) {
                            byte[] buffer = new byte[packet.size()]
                            data.get(buffer)

                            if (streamIndex == avData.audioStreamIndex) {
                                // 1. Clone the packet so recordAAC doesn't alter the original pointers
                                AVPacket clonedPacket = av_packet_clone(packet)
                                if (clonedPacket != null) {
                                    if (avData.audioChannels > 0) {
                                        if (avData.inputAudioCodecName == "aac")
                                            recordAAC.recordPacket(clonedPacket)
                                        else if (avData.inputAudioCodecName == 'pcm_alaw')
                                            mediaStreamHandler.sendPacket("cam4-stream1a", buffer)
                                            //logService.cam.info("pcm packet len = " + buffer.length)
                                    }

                                    av_packet_free(clonedPacket) // Always free native clones to avoid memory leaks
                                }
                            } else if (streamIndex == avData.videoStreamIndex) {
                                 mediaStreamHandler.sendPacket("cam4-stream1", buffer, (packet.flags() & AV_PKT_FLAG_KEY) != 0)
                            }

                            // 2. RECORD TO FLV ONCE PER VALID PACKET
                            // Restore original metadata header values just in case
                            packet.pts(origPts)
                            packet.dts(origDts)
                            packet.duration(origDuration)
                            packet.stream_index(origStreamIndex)

                            recorderFLV.recordPacket(packet)

                            byte[] array = flvOutputStream.toByteArray()

                            if (array.length > 0) {
                               // logService.cam.info("flv output size = " + flvOutputStream.size())
                                flvOutputStream.reset()
                            }

                            if (avData.audioChannels > 0 && avData.inputAudioCodecName == "aac" && aacOutputStream.size() > 0) {
                                byte[] aacArray = aacOutputStream.toByteArray()
                                logService.cam.info("aac output size = " + aacArray.length)
                                aacOutputStream.reset()
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
                Thread.sleep(2000) // Thread will now safely reach here and retry            }
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
}
