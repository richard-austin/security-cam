package server

import grails.plugin.springsecurity.annotation.Secured

import security.cam.onvif.DiscoveryManager
import security.cam.onvif.OnvifManager
import security.cam.onvif.listeners.DiscoveryListener
import security.cam.onvif.listeners.OnvifMediaProfilesListener
import security.cam.onvif.listeners.OnvifMediaStreamURIListener
import security.cam.onvif.listeners.OnvifResponseListener
import security.cam.onvif.listeners.OnvifServicesListener
import security.cam.onvif.models.Device
import security.cam.onvif.models.OnvifDevice
import security.cam.onvif.models.OnvifMediaProfile
import security.cam.onvif.models.OnvifServices
import security.cam.onvif.responses.OnvifResponse

import javax.annotation.Nonnull

class OnvifController {
    @Secured(['ROLE_CLIENT'])
    def discover() {

        OnvifManager onvifManager = new OnvifManager();
        DiscoveryManager manager = new DiscoveryManager();
        manager.setDiscoveryTimeout(10000);
    //    OnvifDevice ovDevice

        manager.discover(new DiscoveryListener() {
            @Override
            void onDiscoveryStarted() {
                System.out.println("Discovery started");
            }

            @Override
            void onDevicesFound(List<Device> devices) {
                for (Device device : devices) {
                    device.setHostName(device.getHostName()+ ':8080')
                    System.out.println("Devices found: " + device.getHostName());
//                    onvifManager.setOnvifResponseListener(new OnvifResponseListener() {
//                        @Override
//                        void onResponse(OnvifDevice onvifDevice, OnvifResponse response) {
//                            System.out.println("Response received: ");
//                        }
//
//                        @Override
//                        void onError(OnvifDevice onvifDevice, int errorCode, String errorMessage) {
//                            System.out.println("Error received: ");
//
//                        }
//                    });

                    OnvifDevice ovDevice = new OnvifDevice(device.getHostName(), '', '')
                    onvifManager.getMediaProfiles(ovDevice, new OnvifMediaProfilesListener() {
                        @Override
                        void onMediaProfilesReceived(@Nonnull OnvifDevice onvifDevice,
                                                            @Nonnull List<OnvifMediaProfile> mediaProfiles) {


                            for(OnvifMediaProfile profile: mediaProfiles) {
                                System.out.println("Media profile found: ${profile.getName()} -- ${profile.getToken()}");
                                onvifManager.getMediaStreamURI(ovDevice, profile, new OnvifMediaStreamURIListener() {
                                    @Override
                                    void onMediaStreamURIReceived(@Nonnull OnvifDevice onvifDevice2,
                                                                         @Nonnull OnvifMediaProfile mediaprofile, @Nonnull String uri) {
                                        System.out.println("Stream uri found: ${uri}");
                                    }
                                });
                            }
                        }
                    });

                    onvifManager.getServices(ovDevice, new OnvifServicesListener() {
                        @Override
                        void onServicesReceived(@Nonnull OnvifDevice onvifDevice, OnvifServices services) {
                            System.out.println(" Services found:-")
                            System.out.println(" Device info path: ${services.getDeviceInformationPath()}")
                            System.out.println(" Profiles path: ${services.getProfilesPath()}")
                            System.out.println(" Service path: ${services.getServicesPath()}")
                            System.out.println(" StreamURI path: ${services.getStreamURIPath()}")
                        }
                    });
                }

            }
        })


        render ''
    }
}
