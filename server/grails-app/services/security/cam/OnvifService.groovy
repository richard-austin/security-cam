package security.cam

import com.google.gson.internal.LinkedTreeMap
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import onvif.discovery.OnvifDiscovery
import onvif.soap.OnvifDevice
import org.onvif.ver10.schema.PTZNode
import org.onvif.ver10.schema.PTZPreset
import org.onvif.ver10.schema.PTZSpaces
import org.onvif.ver10.schema.Space1DDescription
import org.onvif.ver10.schema.Space2DDescription
import security.cam.commands.MoveCommand
import security.cam.commands.MoveCommand.eMoveDirections
import security.cam.commands.PTZPresetsInfoCommand
import security.cam.commands.PTZPresetsCommand
import security.cam.commands.StopCommand

import org.onvif.ver10.media.wsdl.Media
import org.onvif.ver10.schema.AudioEncoderConfiguration
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

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

class SC_PTZData {
    Integer maxPresets
    Media media
    Profile profile
    PTZSpaces spaces
    Map<eMoveDirections, XYZValues> xyzMap
}

@Transactional
class OnvifService {
    LogService logService
    GrailsApplication grailsApplication
    CamService camService
    Sc_processesService sc_processesService
    private ExecutorService deviceUpdateExecutor = Executors.newSingleThreadExecutor()

    OnvifService() {
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

    private synchronized OnvifDevice getDevice(String onvifBaseAddress) {
        try {
            if (!deviceMap.containsKey(onvifBaseAddress))
                deviceMap.put(onvifBaseAddress, new OnvifDevice(onvifBaseAddress, "", ""))
        }
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in getDevice ${ex.getMessage()}")
        }
        deviceMap.get(onvifBaseAddress)
    }

    /**
     * updateDevice: Replace the OnvifDevice entry for onvifBaseAddress when there has been an error in a PTZ call
     * @param onvifBaseAddress
     * @return
     */
    private void updateDevice(final String onvifBaseAddress) {
        deviceMap.remove(onvifBaseAddress)
        // getDevice can be quite slow creating a new device so run it in a thread so it doesn't cause a delay in returning
        deviceUpdateExecutor.submit(() -> {
            getDevice(onvifBaseAddress)  // Replace he removed entry in the map
        })
    }

    ObjectCommandResponse move(MoveCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {

            OnvifDevice device = getDevice(cmd.onvifBaseAddress)

            Profile profile = ptzDataMap.get(cmd.onvifBaseAddress).getProfile()

            if (profile != null) {
                PTZ ptz = device.getPtz()
                PTZSpeed ptzSpd = new PTZSpeed()
                if (cmd.getMoveDirection() == eMoveDirections.panRight || cmd.getMoveDirection() == eMoveDirections.panLeft ||
                        cmd.moveDirection == eMoveDirections.tiltDown || cmd.moveDirection == eMoveDirections.tiltUp) {
                    Vector2D panTilt = new Vector2D()
                    XYZValues xyzValues = ptzDataMap.get(cmd.getOnvifBaseAddress()).xyzMap.get(cmd.getMoveDirection())
                    panTilt.setX(xyzValues.getX())
                    panTilt.setY(xyzValues.getY())
                    ptzSpd.setPanTilt(panTilt)
                } else if (cmd.getMoveDirection() == eMoveDirections.zoomIn || cmd.getMoveDirection() == eMoveDirections.zoomOut) {
                    Vector1D zoom = new Vector1D()
                    XYZValues xyzValues = ptzDataMap.get(cmd.getOnvifBaseAddress()).xyzMap.get(cmd.getMoveDirection())
                    zoom.setX(xyzValues.getZ())
                    ptzSpd.setZoom(zoom)
                }
                ptz.continuousMove(profile.getToken(), ptzSpd, null)
            } else
                throw new Exception("Device ${cmd.onvifBaseAddress} has no media profiles")

        }
        catch (Exception ex) {
            updateDevice(cmd.getOnvifBaseAddress())
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

            Profile profile = ptzDataMap.get(cmd.onvifBaseAddress).getProfile()

            if (profile != null) {
                PTZ ptz = device.getPtz()
                ptz.stop(profile.getToken(), true, true)
            } else
                throw new Exception("Device ${cmd.onvifBaseAddress} has no media profiles")

        }
        catch (Exception ex) {
            updateDevice(cmd.getOnvifBaseAddress())
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

            Profile profile = ptzDataMap.get(cmd.onvifBaseAddress).getProfile()

            if (profile != null) {
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
            updateDevice(cmd.getOnvifBaseAddress())
            result.error = "Error in preset function: ${ex.getMessage()}"
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        return result
    }

    private final Map<String, SC_PTZData> ptzDataMap = new HashMap<>()

    ObjectCommandResponse ptzPresetsInfo(PTZPresetsInfoCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            OnvifDevice device = getDevice(cmd.onvifBaseAddress)
            Media media = device.getMedia()

            Profile profile = media.getProfiles().get(0)
            PTZ ptz = device.getPtz()
            List<PTZPreset> allPresets = ptz.getPresets(profile.getToken())
            List<PTZNode> nodes = ptz.getNodes()
            if (nodes.size() > 0) {
                PTZNode node = nodes.get(0)
                PTZSpaces spaces = node.supportedPTZSpaces
                final int maxPresets = node.getMaximumNumberOfPresets()

                // Set up the map containing the xyz (velocity) values for all ptz operations and directions
                final Map<eMoveDirections, XYZValues> xyzMap = new HashMap<eMoveDirections, XYZValues>()
                // TODO: What do we do if there is more than one of these spaces
                Space2DDescription cptvs = spaces.continuousPanTiltVelocitySpace[0]
                // TODO: What do we do if there is more than one of these spaces
                Space1DDescription czvs = spaces.continuousZoomVelocitySpace[0]

                xyzMap.put(eMoveDirections.panLeft, new XYZValues(cptvs.XRange.min, 0, 0))
                xyzMap.put(eMoveDirections.panRight, new XYZValues(cptvs.XRange.max, 0, 0))
                xyzMap.put(eMoveDirections.tiltDown, new XYZValues(0, cptvs.YRange.min, 0))
                xyzMap.put(eMoveDirections.tiltUp, new XYZValues(0, cptvs.YRange.max, 0))
                xyzMap.put(eMoveDirections.zoomIn, new XYZValues(0, 0, czvs.XRange.max))
                xyzMap.put(eMoveDirections.zoomOut, new XYZValues(0, 0, czvs.XRange.min))

                // Set up the arrays (up to max size 32) for the preset tokens
                List<PTZPreset> presets = new ArrayList()
                for (int i = 0; i < maxPresets && i < allPresets.size(); ++i) {
                    PTZPreset preset = allPresets.get(i)
                    if (preset.getToken() == null || preset.token == "")
                        preset.token = (i + 1).toString()
                    presets.add(allPresets.get(i))
                }
                // Save (or update) the PTZ data for this camera to the map for use by the move and preset methods
                SC_PTZData ptzData = new SC_PTZData()
                ptzData.spaces = spaces
                ptzData.maxPresets = maxPresets
                ptzData.media = media
                ptzData.profile = profile
                ptzData.xyzMap = xyzMap
                ptzDataMap.put(cmd.getOnvifBaseAddress(), ptzData)
                result.responseObject = [maxPresets: maxPresets, presets: presets]
            } else
                throw new Exception("Device ${cmd.onvifBaseAddress} has no ptz nodes")
        }
        catch (Exception ex) {
            updateDevice(cmd.getOnvifBaseAddress())
            result.error = "Error in ptzPresetsInfo: ${ex.getMessage()}"
            logService.cam.error(result.error)
            result.status = PassFail.FAIL
        }
        return result
    }
}
