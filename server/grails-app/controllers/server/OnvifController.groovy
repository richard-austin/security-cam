package server

import grails.plugin.springsecurity.annotation.Secured
import onvif.discovery.OnvifDiscovery
import onvif.soap.OnvifDevice
import onvif.utils.OnvifUtils
import org.apache.commons.io.FileUtils
import org.onvif.ver10.media.wsdl.Media
import org.onvif.ver10.schema.AudioEncoderConfiguration
import org.onvif.ver10.schema.AudioOutputConfiguration
import org.onvif.ver10.schema.Profile
import org.utils.OnvifCredentials
import org.utils.TestDevice
import security.cam.LogService

class OnvifController {
    LogService logService

    static String getDetail(String details, String field)
        throws Exception
    {
        int startIndex = details.indexOf(field), endIndex
        if(startIndex == -1)
            throw new Exception("No such field ${field}")
        startIndex = details.indexOf('[', startIndex)
        if(startIndex == -1)
            throw new Exception('No opening [ found')
        endIndex = startIndex

        String restOfDetails = details.substring(startIndex)
        // Find corresponding closing ]

        int bracketCount = 1, idx = 1
        while (bracketCount > 0 && endIndex < details.length())
        {
            String nextChar = restOfDetails[idx]
            if(nextChar == '[')
                ++bracketCount
            else if(nextChar == ']')
                --bracketCount

            endIndex = startIndex + ++idx
        }
        if(endIndex >= details.length())
            throw new Exception('Could not find matching closing ]')
        return details.substring(startIndex, endIndex)
    }

    @Secured(['ROLE_CLIENT'])
    def discover() {
        Collection<URL> urls = OnvifDiscovery.discoverOnvifURLs()
        List<OnvifCredentials> creds = []
        for (URL u : urls) {
            logService.cam.info(u.toString())
            OnvifCredentials c = new OnvifCredentials(u.host.toString() + ':' + u.port.toString(), 'admin', 'R@nc1dTapsB0ttom', 'MediaProfile000')
            creds.add(c)
        }

        final String propFileRelativePath = "/home/richard/onvif/onvif-java/src/test/resources/onvif.properties"
        final Properties config = new Properties()
        final File f = new File(propFileRelativePath)
        if (!f.exists()) throw new Exception("fnf: " + f.getAbsolutePath())
        config.load(new FileInputStream(f))

        creds.forEach({ credentials ->
            if (credentials != null) {
                OnvifDevice cam = null
                try {

                    System.out.println("Connect to camera, please wait ...")
                    cam = new OnvifDevice(credentials.getHost(), credentials.getUser(), credentials.getPassword())

                    Media media = cam.getMedia()
                    media.getVideoSources()
                    List<Profile> profiles = media.getProfiles()

                    profiles.forEach({Profile profile ->
                        String profileToken = profile.getToken();
                        String name = profile.getName()
                        String rtsp = cam.getStreamUri(profileToken)
                        AudioOutputConfiguration audio1

                        List<AudioEncoderConfiguration> audio2 =  media.getCompatibleAudioEncoderConfigurations(profileToken)
 //                       audio1 =  media.getAudioOutputConfiguration(audio2[0].getToken())
                        AudioEncoderConfiguration aconfig = media.getAudioEncoderConfiguration(audio2[0].getToken())
                        def info = cam.getDeviceInfo()
                        def details = OnvifUtils.format(profile)
                        def x = getDetail(details, 'videoEncoderConfiguration')
                        def y = getDetail(x, 'resolution')
                        def z = y
                    })

                    System.out.printf("Connected to device %s (%s)%n", cam.getDeviceInfo(), cam.streamUri.toString())
                    System.out.println(TestDevice.inspect(cam))

                    String snapshotUri = cam.getSnapshotUri()
                    if (!snapshotUri.isEmpty()) {
                        File tempFile = File.createTempFile("tmp", ".jpg")

                        // Note: This will likely fail if the camera/device is password protected.
                        // embedding the user:password@ into the URL will not work with FileUtils.copyURLToFile
                        FileUtils.copyURLToFile(new URL(snapshotUri), tempFile)
                        System.out.println(
                                "snapshot: " + tempFile.getAbsolutePath() + " length:" + tempFile.length())
                    }

                } catch (Exception th) {
                    System.err.println("Error on device: ${cam?.streamUri?.toString()} ${th.getMessage()}")
                }
            }
        })

        render ''
    }
}
