package security.cam

import com.google.gson.internal.LinkedTreeMap
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import onvif.discovery.OnvifDiscovery
import onvif.soap.OnvifDevice
import org.onvif.ver10.schema.PTZNode
import org.onvif.ver20.ptz.wsdl.Capabilities
import security.cam.commands.MoveCommand
import security.cam.commands.MoveCommand.eMoveDirections
import security.cam.commands.PTZMaxPresetsCommand
import security.cam.commands.PTZPresetsCommand
import security.cam.commands.StopCommand

import javax.xml.datatype.DatatypeFactory
import org.onvif.ver10.media.wsdl.Media
import org.onvif.ver10.schema.AudioEncoderConfiguration
import org.onvif.ver10.schema.AudioSourceConfiguration
import org.onvif.ver10.schema.PTZConfiguration
import org.onvif.ver10.schema.PTZSpeed
import org.onvif.ver10.schema.Profile
import org.onvif.ver10.schema.Vector1D
import org.onvif.ver10.schema.Vector2D
import org.onvif.ver10.schema.VideoEncoderConfiguration
import org.onvif.ver10.schema.VideoResolution
import org.onvif.ver20.ptz.wsdl.PTZ
import org.utils.OnvifCredentials
import org.utils.TestDevice
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse
import server.Camera
import server.Stream

import javax.xml.datatype.Duration

class XYZValues {
    float x
    float y
    float z

    XYZValues(float x, float y, float z) {
        this.x = x
        this.y = y
        this.z = z
    }
}


@Transactional
class OnvifService {
    LogService logService
    GrailsApplication grailsApplication
    CamService camService
    Sc_processesService sc_processesService
    final Map<eMoveDirections, XYZValues> xyzMap = new HashMap<eMoveDirections, XYZValues>()

    OnvifService() {
        xyzMap.put(eMoveDirections.panLeft, new XYZValues(-0.5, 0, 0))
        xyzMap.put(eMoveDirections.panRight, new XYZValues(0.5, 0, 0))
        xyzMap.put(eMoveDirections.tiltDown, new XYZValues(0, -0.5, 0))
        xyzMap.put(eMoveDirections.tiltUp, new XYZValues(0, 0.5, 0))
        xyzMap.put(eMoveDirections.zoomIn, new XYZValues(0, 0, 0.5))
        xyzMap.put(eMoveDirections.zoomOut, new XYZValues(0, 0, -0.5))
    }

    def populateDeviceMap() {
        deviceMap.clear()
        def getCamerasResult = camService.getCameras()
        // Populate the Onvif device map
        def cameras = getCamerasResult.getResponseObject()
        cameras.forEach((k, cam) -> {
            getDevice(cam.onvifHost as String)
        })
    }

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
                    cam.onvifHost = credentials.host
                    cam.streams = new LinkedTreeMap<String, Stream>()

                    try {
                        logService.cam.info "Creating onvif device for ${credentials.getHost()} ..."
                        device = getDevice(credentials.getHost())

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

                            //  AudioSourceConfiguration asc = profile.getAudioSourceConfiguration()
                        })
                        logService.cam.info("Connected to device %s (%s)%n", device.getDeviceInfo(), device.streamUri.toString())
                        logService.cam.info(TestDevice.inspect(device))

                        String snapshotUri = device.getSnapshotUri()
                        if (!snapshotUri.isEmpty()) {
                            cam.snapshotUri = snapshotUri
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
            result.error = "IO Error connecting to camera at ${strUrl}: ${ex.getMessage()}"
            if (uc != null) {
                try {
                    result.errno = uc.getResponseCode()
                }
                catch (Exception ignore) {
                    result.errno = 500
                }
            }

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

    private final static Map<String, OnvifDevice> deviceMap = new HashMap<>()

    private static OnvifDevice getDevice(String onvifBaseAddress) {
        if (!deviceMap.containsKey(onvifBaseAddress))
            deviceMap.put(onvifBaseAddress, new OnvifDevice(onvifBaseAddress, "", ""))

        deviceMap.get(onvifBaseAddress)
    }

    ObjectCommandResponse move(MoveCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            OnvifDevice device = getDevice(cmd.onvifBaseAddress)

            Media media = device.getMedia()
            List<Profile> profiles = media.getProfiles()

            if (profiles.size() > 0) {
                Profile profile = profiles[0]

                PTZ ptz = device.getPtz()
                PTZSpeed ptzSpd = new PTZSpeed()
                Vector2D panTilt = new Vector2D()
                Vector1D zoom = new Vector1D()
                XYZValues xyzValues = xyzMap.get(cmd.getMoveDirection())
                panTilt.setX(xyzValues.getX())
                panTilt.setY(xyzValues.getY())
                zoom.setX(xyzValues.getZ())
                ptzSpd.setPanTilt(panTilt)
                ptzSpd.setZoom(zoom)
                ptz.continuousMove(profile.getToken(), ptzSpd, null)
            } else
                throw new Exception("Device ${cmd.onvifBaseAddress} has no media profiles")

        }
        catch (Exception ex) {
            result.error = "Error in move: ${ex.getMessage()}"
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        return result
    }

    ObjectCommandResponse stop(StopCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            OnvifDevice device = getDevice(cmd.onvifBaseAddress)

            Media media = device.getMedia()
            // def options =media.getVideoSources()
            List<Profile> profiles = media.getProfiles()

            if (profiles.size() > 0) {
                Profile profile = profiles[0]

                PTZ ptz = device.getPtz()
                ptz.stop(profile.getToken(), true, true)
            } else
                throw new Exception("Device ${cmd.onvifBaseAddress} has no media profiles")

        }
        catch (Exception ex) {
            result.error = "Error in stop: ${ex.getMessage()}"
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        return result
    }

    ObjectCommandResponse preset(PTZPresetsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            OnvifDevice device = getDevice(cmd.onvifBaseAddress)

            Media media = device.getMedia()
            // def options =media.getVideoSources()
            List<Profile> profiles = media.getProfiles()

            if (profiles.size() > 0) {
                Profile profile = profiles[0]

                PTZ ptz = device.getPtz()
                switch (cmd.operation) {
                    case PTZPresetsCommand.ePresetOperations.moveTo:
                        ptz.gotoPreset(profile.getToken(), cmd.preset, null)
                        break
                    case PTZPresetsCommand.ePresetOperations.saveTo:
                        ptz.setPreset(profile.getToken(), cmd.preset, null)
                        break
                    case PTZPresetsCommand.ePresetOperations.clearFrom:
                        ptz.removePreset(profile.getToken(), cmd.preset)
                        break
                    default:
                        throw new Exception("Invalid preset operation ${cmd.operation}")
                        break
                }
            } else
                throw new Exception("Device ${cmd.onvifBaseAddress} has no media profiles")
        }
        catch (Exception ex) {
            result.error = "Error in preset function: ${ex.getMessage()}"
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        return result
    }

    ObjectCommandResponse maxNumberOfPresets(PTZMaxPresetsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            OnvifDevice device = getDevice(cmd.onvifBaseAddress)
            PTZ ptz = device.getPtz()

            List<PTZNode> nodes = ptz.getNodes()
            if (nodes.size() > 0) {
                PTZNode node = nodes.get(0)
                result.responseObject = node.getMaximumNumberOfPresets()
            } else
                throw new Exception("Device ${cmd.onvifBaseAddress} has no ptz nodes")
        }
        catch (Exception ex) {
            result.error = "Error in maxNumberOfPresets: ${ex.getMessage()}"
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        return result
    }
}
