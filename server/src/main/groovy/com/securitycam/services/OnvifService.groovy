package com.securitycam.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import com.securitycam.audiobackchannel.RtspClient
import com.securitycam.commands.DiscoverCameraDetailsCommand
import com.securitycam.commands.PTZPresetsCommand
import com.securitycam.commands.PtzCommand
import com.securitycam.commands.SetOnvifCredentialsCommand
import com.securitycam.commands.UpdateCamerasCommand
import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.controllers.RecordingType
import com.securitycam.controllers.Stream
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.PullPointHandlerContainer
import common.Authentication
import de.onvif.discovery.OnvifDiscovery
import de.onvif.soap.OnvifDevice
import org.apache.commons.io.IOUtils
import org.apache.http.protocol.BasicHttpContext
import org.onvif.ver10.media.wsdl.Media
import org.onvif.ver10.schema.AudioEncoderConfiguration
import org.onvif.ver10.schema.PTZNode
import org.onvif.ver10.schema.PTZPreset
import org.onvif.ver10.schema.PTZSpaces
import org.onvif.ver10.schema.PTZSpeed
import org.onvif.ver10.schema.Profile
import org.onvif.ver10.schema.Space1DDescription
import org.onvif.ver10.schema.Space2DDescription
import org.onvif.ver10.schema.Vector1D
import org.onvif.ver10.schema.Vector2D
import org.onvif.ver10.schema.VideoEncoderConfiguration
import org.onvif.ver10.schema.VideoResolution
import org.onvif.ver20.ptz.wsdl.PTZ
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import de.onvif.utils.OnvifCredentials
import com.securitycam.commands.MoveCommand
import de.onvif.utils.TestDevice

import javax.net.ssl.*
import jakarta.xml.ws.WebServiceException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.CopyOnWriteArrayList
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
    Map<MoveCommand.eMoveDirections, XYZValues> xyzMap
}

class DiscoveryResult {
    Map<String, Camera> cams
    Map<String, String> failed
}

@Service
class OnvifService {
    @Autowired
    LogService logService
    @Autowired
    CamService camService
    @Autowired
    Sc_processesService sc_processesService
    @Autowired
    Config config
    @Autowired
    ConfigurationUpdateService configurationUpdateService
    @Autowired
    RestfulInterfaceService restfulInterfaceService

    private ExecutorService deviceUpdateExecutor = Executors.newSingleThreadExecutor()

    def populateDeviceMap() {
        deviceMap.clear()
        def getCamerasResult = camService.getCameras()
        if (getCamerasResult.status == PassFail.PASS) {
            // Populate the Onvif device map
            def cameras = getCamerasResult.responseObject as Map<String, Camera>
            Asymmetric asym = new Asymmetric()
            cameras.forEach((k, cam) -> {
                String jsonCreds = asym.decrypt(cam.cred as String)
                ObjectMapper mapper = new ObjectMapper()
                def (user, password) = [null, null]
                if (jsonCreds.length() > 0) {
                    CameraAdminCredentials cac = mapper.readValue(jsonCreds, CameraAdminCredentials.class)
                    user = cac.userName
                    password = cac.password
                }
                getDevice(cam.onvifHost as String, user, password)
            })
        } else
            throw new Exception("Error in populateDeviceMap: " + getCamerasResult.error)
    }

    /**
     * getOnvifCredentials: Get the user name and password to authenticate on the cameras Onvif services.
     * These are set up with the setOnvifCredentials call
     *
     * @return The user name
     */
    def getOnvifCreds() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            FileInputStream fis

            fis = new FileInputStream("${config.camerasHomeDirectory}/onvifCredentials.json")

            String data = IOUtils.toString(fis, "UTF-8")
            Gson gson2 = new Gson()
            Object obj = gson2.fromJson(data, Object.class)
            response.responseObject = obj
        }
        catch(FileNotFoundException ignore) {
            response.responseObject = [onvifUserName: "", onvifPassword: ""]
        }
        catch (Exception ex) {
            logService.cam.error "getOnvifCredentials() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }
        return response
    }


    private def getOnvifCredentials() {
        def user = ""
        def password = ""
        def response = getOnvifCreds()

        if (response.status == PassFail.PASS) {
            user = response.responseObject?.onvifUserName
            password = response.responseObject?.onvifPassword
        }
        return [user, password]
    }

    /**
     * getMediaProfiles: Get the details of Onvif compliant cameras which are online on the LAN.
     * @return: LinkedHashMap<String, Camera> containing discovered cameras with all fields populated which can be.
     */
    def getMediaProfiles(DiscoverCameraDetailsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            List<OnvifCredentials> creds = []
            sc_processesService.stopProcesses()
            Collection<URL> urls = new CopyOnWriteArrayList<>()
            deviceMap.clear()
            if (cmd?.onvifUrl == null) {  // Discover cameras on LAN with multicast probe
                logService.cam.info "Camera discovery..."
                urls = OnvifDiscovery.discoverOnvifURLs()
            } else { // Get the details for the camera with the given Onvif URL
                urls.add(new URI(cmd?.onvifUrl).toURL())
            }
            def user, password
            if (cmd?.onvifUserName != null && cmd.onvifUserName != "")
                (user, password) = [cmd.onvifUserName, cmd.onvifPassword]
            else
                (user, password) = getOnvifCredentials()

            for (URL u : urls) {
                logService.cam.info(u.toString())

                OnvifCredentials c = new OnvifCredentials(u.protocol + "://" + u.host.toString() + ':' + u.port.toString() + u.path, user, password, 'MediaProfile000')
                creds.add(c)
            }

            Map<String, Camera> cams = new LinkedTreeMap<String, Camera>()
            final Map<String, String> failed = new HashMap<String, String>()
            int camNum = 0
            creds.forEach({ credentials ->
                if (credentials != null) {
                    OnvifDevice device = null
                    Camera cam = new Camera()
                    cam.onvifHost = credentials.host
                    cam.streams = new LinkedTreeMap<String, Stream>()
                    Asymmetric asym = new Asymmetric()
                    if(!credentials.nullOrEmpty())
                        cam.cred = asym.encrypt("{\"userName\": \"${credentials.user}\", \"password\": \"${credentials.password}\"}")
                    else
                        cam.cred = ""
                    RtspClient rtspClient =
                            new RtspClient(
                                    getHostFromHostPort(credentials.getHost()),
                                    554,
                                    credentials.user,
                                    credentials.password,
                                    logService,
                                    cam)
                    rtspClient.start()
                    rtspClient.await()


                    try {
                        logService.cam.info "Creating onvif device for ${credentials.getHost()} ..."
                        device = getDevice(credentials.getHost(), user, password)
                        if (device == null)
                            throw new Exception("No camera found at ${cmd?.onvifUrl == null ? credentials.host : cmd.onvifUrl}")

                        List<Profile> profiles
                        try {
                            Media media = device.getMedia()
                            profiles = media.getProfiles()
                        }
                        catch (WebServiceException ex) {
                            // Add this one to the list of
                            failed.put(credentials.getOnvifUrl(), "Failed to get media profile: ${ex.getMessage()}")
                            return
                        }
                        catch(Exception ex) {
                            failed.put(credentials.getOnvifUrl(), "${ex.getClass()} when getting media profiles: ${ex.getMessage()}: ${ex.getCause()}")
                            return
                        }

                        int streamNum = 0

                        if(profiles != null) {
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
                                if (aec != null) {
                                    int bitRate = aec.getBitrate()
                                    // bitRate should be in Kbps, though it is in bps from SV3C type cameras.
                                    if (bitRate.intValue() < 200)
                                        bitRate *= 1000
                                    stream.audio_bitrate = bitRate
                                    String encoding = aec.getEncoding().value()
                                    if (isSupportedAudioOutputFmt(encoding) && bitRate.intValue() != 0) {
                                        stream.audio_encoding = encoding
                                        stream.audio = true
                                    } else {
                                        stream.audio_encoding = "None"
                                        stream.audio = false
                                    }
                                    stream.audio_sample_rate = aec.getSampleRate().intValue()
                                    // sampleRate should be in Kbps, though it is in bps from SV3C type cameras.
                                    if (stream.audio_sample_rate < 200) {
                                        stream.audio_sample_rate = stream.audio_sample_rate.intValue() * 1000
                                    }

                                    //  AudioSourceConfiguration asc = profile.getAudioSourceConfiguration()
                                } else {
                                    stream.audio_encoding = "None"
                                    stream.audio = false
                                    stream.audio_sample_rate = stream.audio_bitrate = 0
                                }
                            })
                        }
                        logService.cam.info("Connected to device "+device.getDeviceInfo().manufacturer +": "+device.getDeviceInfo().model + ": at "+ device.streamUri.toString())
                        logService.cam.debug(TestDevice.inspect(device))

                        String snapshotUri = device.getSnapshotUri()
                        if (!snapshotUri.isEmpty()) {
                            // If port 80 is specified for an http url, this will cause digest auth to fail,
                            //  so remove it as it's implied by the http:// protocol heading
                            if (snapshotUri.startsWith("http://"))
                                snapshotUri = snapshotUri.replace(":80/", "/")

                            cam.snapshotUri = snapshotUri
                        }

                    } catch (Exception th) {
                        logService.cam.error("Error on device: ${device?.streamUri?.toString()}: ${th.getClass().getName()}: ${th.getMessage()}")
                        result.status = PassFail.FAIL
                        result.error = "Error processing Onvif device responses " + th.getMessage()
                    }
                    // Set lowest resolution stream for default on multi display and for motion detection
                    setDefaults(cam)

                    PullPointHandlerContainer hc = new PullPointHandlerContainer(device, "", cam, restfulInterfaceService)
                    hc.getEvents()
                    cams.put('camera' + ++camNum, cam)
                }
            })
            result.responseObject = new DiscoveryResult(cams: cams, failed: failed)
        }
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in getMediaProfiles: ${ex.getMessage()}")
            result.status = PassFail.FAIL
            result.error = "Error processing Onvif responses " + ex.getMessage()
            deviceMap.clear()  // Ensure credentials will be set up again
        }
        finally {
            // If this was for a specific camera, remove from the device map as the credentials may differ from the global creds
            if (cmd?.onvifUrl != null) {
                def url = new URI(cmd.onvifUrl).toURL()
                def onvifHost = url.host+":"+url.port
                deviceMap.remove(onvifHost)
            }
            sc_processesService.startProcesses()
        }
        return result
    }

    private static boolean isSupportedAudioOutputFmt(String format) {
        final String supportedFmtsRegex = /^(AAC|G711)$/  /* G726 */
        return format.matches(supportedFmtsRegex)
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
        int indexOfForwardSlash = urlParts[1].indexOf('/')
        if (indexOfForwardSlash == -1)
            indexOfForwardSlash = urlParts[1].length()
        if (indexOfColon == -1 || indexOfForwardSlash < indexOfColon)
            return urlParts[1].substring(0, indexOfForwardSlash)

        return urlParts[1].substring(0, indexOfColon)
    }

    /**
     * getPortFromHost: Get the rtsp port number for host string of the form, <host or ip>:<port>
     *                  If port is not present, return the default rtsp port 554
     * @param host : Host (format <host or ip>:<port> or <host or ip>
     * @return The rtsp port number
     * @throws Exception
     */
    private static int getPortFromHost(String host)
            throws Exception {
        String port = 554
        String[] hostParts = host.split(':')
        if (hostParts.length == 2)
            port = hostParts[1]
        return Integer.parseInt(port)
    }

    private static String getHostFromHostPort(String hostPort) throws Exception {
        String[] hostParts = hostPort.split(':')
        if (hostParts.length > 0)
            return hostParts[0]
        else
            throw new Exception("Host incorrect in getHostFromHostPort")
    }

    /**
     * getBaseUrl: Get the protocol/address/port part of the url with no uri
     * @param url : The rtsp url
     * @return: The base url
     */
    private static String getBaseUrl(String url) {
        String[] urlParts = url.split('//')
        String[] urlBreakDown = urlParts[1].split("/")
        return urlParts[0] + "//" + urlBreakDown[0]
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
        }
    }

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                X509Certificate[] getAcceptedIssuers() {
                    return null
                }

                void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                }

                void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                }
            }
    }

    def getSnapshot(String url, String cred) {
        ObjectCommandResponse resp = getSnapshotWithAuth(url, "")
        try {
            if (resp.errno == 401) {
                Authentication auth = new Authentication(logService)
                Asymmetric asym = new Asymmetric()
                var jsonCreds = asym.decrypt(cred)
                CameraAdminCredentials creds = new CameraAdminCredentials()
                ObjectMapper mapper = new ObjectMapper()
                if (jsonCreds.length() > 0)
                    creds = mapper.readValue(jsonCreds, CameraAdminCredentials.class)

                var ah = auth.getAuthResponse(creds.userName, creds.password, "GET", url, resp.response as String, new BasicHttpContext())
                String authString = ah.value
                resp = getSnapshotWithAuth(url, authString)
            }
        }
        catch (Exception ex) {
            resp.status = PassFail.FAIL
            resp.error = "${ex.getClass().getName()} in  getSnapshot: ${ex.getMessage()}"
            logService.cam.error(resp.error)
        }

        return resp
    }

    /**
     * getSnapshot: Get a snapshot from the given URL and save it as a jpg file to the stream1 recording location
     * @param url
     */
    private def getSnapshotWithAuth(String strUrl, String authString) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        SSLContext sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, new SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            boolean verify(String hostname, SSLSession session) {
                return true
            }
        }

        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)

        HttpURLConnection uc = null
        try {
            URL url = new URI(strUrl).toURL()
            uc = url.openConnection() as HttpURLConnection
            uc.setRequestProperty("Authorization", authString)
            InputStream input = uc.getInputStream()
            result.responseObject = input.readAllBytes()
            input.close()
        }
        catch (IOException ex) {
            result.error = "IO Error connecting to camera at ${strUrl}: ${ex.getMessage()}"
            if (uc != null) {
                try {
                   // def rm = uc.getRequestMethod()
                    result.response = uc.getHeaderField("WWW-Authenticate")

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

    private synchronized OnvifDevice getDevice(String onvifBaseAddress, String onvifUserName = "", String onvifPassword = "") {
        if(onvifUserName == null)
            onvifUserName = ""
        if(onvifPassword == null)
            onvifPassword = ""
        try {
            if (!deviceMap.containsKey(onvifBaseAddress)) {
                def user, password
                if(onvifUserName.length()> 0 && onvifPassword.length() > 0)
                    (user, password) = [onvifUserName, onvifPassword]
                else
                    (user, password) = getOnvifCredentials()

                deviceMap.put(onvifBaseAddress, new OnvifDevice(onvifBaseAddress, user, password))
            }
        }
        catch (Exception ex) {
            System.out.println("Exception: ${ex.getClass()} ${ex.getMessage()} ${ex.getCause()}")
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
            OnvifDevice device = getDevice(cmd.onvifBaseAddress, cmd.user, cmd.password)

            Profile profile = ptzDataMap.get(cmd.onvifBaseAddress).getProfile()

            if (profile != null) {
                PTZ ptz = device.getPtz()
                PTZSpeed ptzSpd = new PTZSpeed()
                if (cmd.getMoveDirection() == MoveCommand.eMoveDirections.panRight || cmd.getMoveDirection() == MoveCommand.eMoveDirections.panLeft ||
                        cmd.moveDirection == MoveCommand.eMoveDirections.tiltDown || cmd.moveDirection == MoveCommand.eMoveDirections.tiltUp) {
                    Vector2D panTilt = new Vector2D()
                    XYZValues xyzValues = ptzDataMap.get(cmd.getOnvifBaseAddress()).xyzMap.get(cmd.getMoveDirection())
                    panTilt.setX(xyzValues.getX())
                    panTilt.setY(xyzValues.getY())
                    ptzSpd.setPanTilt(panTilt)
                } else if (cmd.getMoveDirection() == MoveCommand.eMoveDirections.zoomIn || cmd.getMoveDirection() == MoveCommand.eMoveDirections.zoomOut) {
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

    ObjectCommandResponse stop(PtzCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            OnvifDevice device = getDevice(cmd.onvifBaseAddress, cmd.user, cmd.password)

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
            OnvifDevice device = getDevice(cmd.onvifBaseAddress, cmd.user, cmd.password)

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

    ObjectCommandResponse ptzPresetsInfo(PtzCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            OnvifDevice device = getDevice(cmd.onvifBaseAddress, cmd.user, cmd.password)
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
                final Map<MoveCommand.eMoveDirections, XYZValues> xyzMap = new HashMap<MoveCommand.eMoveDirections, XYZValues>()
                // TODO: What do we do if there is more than one of these spaces
                Space2DDescription cptvs = spaces.continuousPanTiltVelocitySpace[0]
                // TODO: What do we do if there is more than one of these spaces
                Space1DDescription czvs = spaces.continuousZoomVelocitySpace[0]

                xyzMap.put(MoveCommand.eMoveDirections.panLeft, new XYZValues(cptvs.XRange.min, 0, 0))
                xyzMap.put(MoveCommand.eMoveDirections.panRight, new XYZValues(cptvs.XRange.max, 0, 0))
                xyzMap.put(MoveCommand.eMoveDirections.tiltDown, new XYZValues(0, cptvs.YRange.min, 0))
                xyzMap.put(MoveCommand.eMoveDirections.tiltUp, new XYZValues(0, cptvs.YRange.max, 0))
                xyzMap.put(MoveCommand.eMoveDirections.zoomIn, new XYZValues(0, 0, czvs.XRange.max))
                xyzMap.put(MoveCommand.eMoveDirections.zoomOut, new XYZValues(0, 0, czvs.XRange.min))

                // Set up the arrays (up to max size 32) for the preset tokens
                List<PTZPreset> presets = new ArrayList()
                for (int i = 0; i < maxPresets && i < allPresets.size() && i < 32; ++i) {
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

    def haveOnvifCredentials() {
        ObjectCommandResponse response = new ObjectCommandResponse()
        try {
            String pw = "", un = ""
            response = getOnvifCreds() as ObjectCommandResponse
            if (response.status == PassFail.PASS) {
                un = response.responseObject?.onvifUserName
                pw = response.responseObject?.onvifPassword
            }
            response.responseObject = !un.isEmpty() && !pw.isEmpty()
        }
        catch (Exception ex) {
            String msg = "${ex.getClass().getName()} in haveCameraCredentials: ${ex.getMessage()}"
            response.responseObject = false
            response.status = PassFail.FAIL
            response.error = msg
            logService.getCam().error(msg)
        }
        return response
    }

    /**
     * setOnvifCredentials: Set the access credentials required by some cameras to return onvif query results
     * @param cmd : Command object containing the username and password
     * @return: ObjectCommandResponse with success/error state.
     */
    def setOnvifCredentials(SetOnvifCredentialsCommand cmd) {
        ObjectCommandResponse response = new ObjectCommandResponse()

        try {
            String json = """{
    \"onvifUserName\": \"${cmd.onvifUserName}\",
    \"onvifPassword\": \"${cmd.onvifPassword}\"
}
"""
            String fileName = "${config.camerasHomeDirectory}/onvifCredentials.json"
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(json)

            writer.close()
        }
        catch (Exception ex) {
            logService.cam.error "setCameraAccessCredentials() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            response.status = PassFail.FAIL
            response.error = ex.getMessage()
        }

        return response
    }

    def updateCameras(UpdateCamerasCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create()
            JsonElement je = JsonParser.parseString(cmd.camerasJSON)
            String prettyJsonString = gson.toJson(je)

            String fileName
            fileName = "${config.camerasHomeDirectory}/cameras.json"

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))
            writer.write(prettyJsonString)

            writer.close()

            ObjectCommandResponse stopResult = sc_processesService.stopProcesses()
            result = configurationUpdateService.generateConfigs()
            ObjectCommandResponse startResult = sc_processesService.startProcesses()

            if (result.status !== PassFail.PASS)
                throw new Exception(result.error)

            Gson gson2 = new Gson()
            LinkedTreeMap<String, Camera> obj = gson2.fromJson(prettyJsonString, Object.class) as LinkedTreeMap<String, Camera>

            removeUnusedMaskFiles(obj, config)

            if (stopResult.status != PassFail.PASS)
                result = stopResult
            else if (startResult.status != PassFail.PASS)
                result = startResult

            // Populate the Onvif device map to prevent the delay of creating the device at the start of each PTZ operation
            populateDeviceMap()
            result.setResponseObject(obj)
        }
        catch (Exception ex) {
            logService.cam.error "Exception in updateCameras: " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    static private def removeUnusedMaskFiles(LinkedTreeMap<String, Camera> jsonObj, Config config) {
        Set<String> mask_files = new HashSet<String>()

        // Make a set of file names which are in use
        for (Map.Entry<String, Camera> cam : jsonObj.entrySet())
            for (Map.Entry<String, Stream> stream : cam.value.streams)
                if (stream.value.motion.enabled && stream.value.motion.mask_file != "")
                    mask_files.add(stream.value.motion.mask_file)

        // Get the .pgm files in the motion directory
        File directory = new File("${config.camerasHomeDirectory}/motion")
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pgm")
            }
        })

        // Remove .pgm files not in the data set
        for (File file : files) {
            if (!mask_files.contains(file.name))
                file.delete()
        }
    }


    def uploadMaskFile(MultipartFile maskFile) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            File file
            file = new File("${config.motion.maskFileDir}/" + maskFile.originalFilename)
            maskFile.transferTo(file)
        }
        catch (Exception ex) { // Some other type of exception
            logService.cam.error "uploadMaskFile() caught " + ex.getClass().getName() + " with message = " + ex.getMessage()
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    def pullPointSubscriptions = new ArrayList<PullPointHandlerContainer>()

    def startPullPointEventMonitor() {
        if(pullPointSubscriptions.size() > 0)
            throw new Exception("Pull point event monitor already started")

        def getCamerasResult = camService.getCameras()
        def cameras = getCamerasResult.responseObject as Map<String, Camera>
        Asymmetric asym = new Asymmetric()
        for(Map.Entry<String, Camera> cam: cameras) {
            final Camera camera = cam.value
            if(camera.recordingType.toString() == RecordingType.pullPointEventTriggered.name()) {
                String jsonCreds = asym.decrypt(camera.cred as String)
                ObjectMapper mapper = new ObjectMapper()
                def (user, password) = [null, null]
                if (jsonCreds.length() > 0) {
                    CameraAdminCredentials cac = mapper.readValue(jsonCreds, CameraAdminCredentials.class)
                    user = cac.userName
                    password = cac.password
                }

                OnvifDevice device = getDevice(camera.onvifHost, user, password)
                def pullPointHandler = new PullPointHandlerContainer(device, cam.key, camera, restfulInterfaceService)
                pullPointHandler.setupSimpleItemTriggerValues(camera.simpleItemName, camera.simpleItemPositiveValue, camera.simpleItemNegativeValue)
                pullPointHandler.subscribe(camera.pullPointTopic)
                pullPointSubscriptions.add(pullPointHandler)
            }
        }
     }

    def stopPullPointEventMonitor() {
        pullPointSubscriptions.forEach(handler -> {
            handler.terminate()
        })
        pullPointSubscriptions.clear()
    }
}
