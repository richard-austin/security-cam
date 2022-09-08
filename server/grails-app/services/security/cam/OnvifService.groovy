package security.cam

import com.google.gson.internal.LinkedTreeMap
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import onvif.discovery.OnvifDiscovery
import onvif.soap.OnvifDevice
import org.onvif.ver10.media.wsdl.Media
import org.onvif.ver10.schema.AudioEncoderConfiguration
import org.onvif.ver10.schema.Profile
import org.onvif.ver10.schema.VideoEncoderConfiguration
import org.onvif.ver10.schema.VideoResolution
import org.utils.OnvifCredentials
import org.utils.TestDevice
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import server.Camera
import server.Stream

import java.nio.charset.Charset

@Transactional
class OnvifService {
    LogService logService
    GrailsApplication grailsApplication
    CamService camService
    Sc_processesService sc_processesService

    /**
     * getMediaProfiles: Get the details of Onvif compliant cameras which are online on the LAN.
     * @return: LinkedHashMap<String, Camera> containing discovered cameras with all fields populated which can be.
     */
    def getMediaProfiles() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            logService.cam.info "Camera discovery..."
            sc_processesService.stopProcesses()
            Collection<URL> urls = OnvifDiscovery.discoverOnvifURLs()
            List<OnvifCredentials> creds = []
            for (URL u : urls) {
                logService.cam.info(u.toString())
                OnvifCredentials c = new OnvifCredentials(u.host.toString() + ':' + u.port.toString(), '', 'R@', 'MediaProfile000')
                creds.add(c)
            }

            Map<String, Camera> cams = new LinkedTreeMap<String, Camera>()
            int camNum = 0
            creds.forEach({ credentials ->
                if (credentials != null) {
                    OnvifDevice device = null
                    Camera cam = new Camera()
                    cam.streams = new LinkedTreeMap<String, Stream>()

                    try {
                        logService.cam.info "Connect to camera, please wait ..."
                        device = new OnvifDevice(credentials.getHost(), credentials.getUser(), credentials.getPassword())

                        Media media = device.getMedia()
                        // def options =media.getVideoSources()
                        List<Profile> profiles = media.getProfiles()

                        int streamNum = 0

                        profiles.forEach({ Profile profile ->
                            Stream stream = new Stream()
                            String profileToken = profile.getToken()
                            cam.streams.put('stream' + ++streamNum, stream)
                            String streamUrl = device.getStreamUri(profileToken)
                            cam.address = getIPFromUrl(streamUrl)
                            stream.netcam_uri = streamUrl

                            VideoEncoderConfiguration vec = profile.getVideoEncoderConfiguration()
                            VideoResolution resolution = vec.resolution
                            stream.video_width = resolution.getWidth()
                            stream.video_height = resolution.getHeight()

                            AudioEncoderConfiguration aec = profile.getAudioEncoderConfiguration()
                            stream.audio_bitrate = aec.getBitrate()
                            stream.audio_encoding = aec.getEncoding().value()
                            stream.audio_sample_rate = aec.getSampleRate()

//                        AudioSourceConfiguration aec = profile.getAudioSourceConfiguration()
//                        PTZConfiguration ptzConf = profile.getPTZConfiguration()
//                        Map ptzProps = ptzConf.getProperties()
//                        PTZ ptz = device.getPtz()
//                        List pre = ptz.getPresets(profile.getToken())
//                        PTZSpeed ptzSpd = new PTZSpeed()
//                        ptzSpd.setZoom(new Vector1D().setX(4))
//                        ptz.absoluteMove(profile.getToken(), pre[3].getPTZPosition(), new PTZSpeed())
//
//                        String name = profile.getName()

//                        List<AudioEncoderConfiguration> audio2 =  media.getCompatibleAudioEncoderConfigurations(profileToken)
//                        //                       audio1 =  media.getAudioOutputConfiguration(audio2[0].getToken())
//                        AudioEncoderConfiguration aconfig = media.getAudioEncoderConfiguration(audio2[0].getToken())
//                        def info = device.getDeviceInfo()
                        })
                        logService.cam.info("Connected to device %s (%s)%n", device.getDeviceInfo(), device.streamUri.toString())
                        logService.cam.info(TestDevice.inspect(device))

                        String snapshotUri = device.getSnapshotUri()
                        if (!snapshotUri.isEmpty()) {
                            cam.snapshotUri = snapshotUri

//                        File tempFile = File.createTempFile("tmp", ".jpg")
//
//                        try {
//                            // Note: This will likely fail if the camera/device is password protected.
//                            // embedding the user:password@ into the URL will not work with FileUtils.copyURLToFile
//                            FileUtils.copyURLToFile(new URL(snapshotUri), tempFile)
//                            logService.cam.info(
//                                    "snapshot: " + tempFile.getAbsolutePath() + " length:" + tempFile.length())
//                        }
//                        catch(Exception ex)
//                        {
//                            logService.cam.error("Cannot get snapshot data from ${snapshotUri} : "+ex.getMessage())
//                        }
                        }

                    } catch (Exception th) {
                        logService.cam.error("Error on device: ${device?.streamUri?.toString()}: ${th.getClass().getName()}: ${th.getMessage()}")
                        result.status = PassFail.FAIL
                        result.error = "Error processing Onvif device responses " + th.getMessage()
                    }
                    // Set lowest resolution stream for default on multi display and for motion detection
                    setDefaults(cam)
                    cams.put('camera' + ++camNum, cam)
                }
            })
            result.responseObject = cams
        }
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in getMediaProfiles: ${ex.getMessage()}")
            result.status = PassFail.FAIL
            result.error = "Error processing Onvif responses " + ex.getMessage()
        }
        finally {
            sc_processesService.startProcesses()
        }
        return result
    }

    /**
     *
     * @param url : The full rtsp url with IP address, of the discovered camera
     * @return: The IP address portion of the URL
     * @throws Exception
     */
    private static String getIPFromUrl(String url)
            throws Exception {
        String[] urlParts = url.split('//')
        int indexOfColon = urlParts[1].indexOf(':')
        int indexOfForwrdSlash = urlParts[1].indexOf('/')
        if (indexOfColon == -1 || indexOfForwrdSlash < indexOfColon)
            return urlParts[1].substring(0, indexOfForwrdSlash)

        return urlParts[1].substring(0, indexOfColon)
    }

    /**
     * setDefaults: Set default values for video_height and width, motion enabled (on the lowest res stream) and
     *              defaultOnMultiDisplay (also on the lowest res stream).
     * @param camera
     */
    private static void setDefaults(Camera camera) {
        int lowestRes = Integer.MAX_VALUE
        String lowestResStream = ""

        camera.streams.forEach({ String key, Stream stream ->
            if (stream.video_height != null && stream.video_width != null &&
                    stream.video_height > 0 && stream.video_width > 0) {
                int thisRes = stream.video_width * stream.video_height
                if (thisRes < lowestRes) {
                    lowestRes = thisRes
                    lowestResStream = key
                }
            }
        })
        if (lowestResStream != "") {
            Stream lrs = camera.streams.get(lowestResStream)
            lrs.defaultOnMultiDisplay = true
            lrs.motion.enabled = true
        }
    }

    /**
     * getSnapshot: Get a snapshot from the given URL and save it as a jpg file to the stream1 recording location
     * @param url
     */
    def getSnapshot(String strUrl) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        HttpURLConnection uc = null
        try {
            URL url = new URL(strUrl)
            uc = url.openConnection() as HttpURLConnection
            String username = camService.cameraAdminUserName()
            String password = camService.cameraAdminPassword()

            String userpass = "${username}:${password}"
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()))
            uc.setRequestProperty("Authorization", basicAuth)
            InputStream input = uc.getInputStream()
            result.responseObject = input.readAllBytes()
            input.close()
//            Files.write(new File("${grailsApplication.config.camerasHomeDirectory}/auto.jpg").toPath(), result.responseObject as byte[])
        }
        catch (IOException ex) {
            result.error = "IO Error in getSnapshot: ${ex.getMessage()}"
            if(uc != null)
                result.errno = uc.getResponseCode()
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        catch (Exception ex) {
            result.error = "Error in getSnapshot: ${ex.getMessage()}"
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        return result
    }
}
