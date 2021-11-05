package server

import grails.plugin.springsecurity.annotation.Secured
import onvif.discovery.OnvifDiscovery
import onvif.soap.OnvifDevice
import org.apache.commons.io.FileUtils
import org.utils.OnvifCredentials
import org.utils.TestDevice
import security.cam.LogService

class OnvifController {
    LogService logService


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
