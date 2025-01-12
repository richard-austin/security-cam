package de.onvif.discovery;

import java.net.URL;
import java.util.Collection;

/**
 * @author th
 * @date 2015-06-18
 */
public class OnvifDiscovery {

  public static Collection<URL> discoverOnvifURLs() {
    return DeviceDiscovery.discoverWsDevicesAsUrls("^http$", ".*onvif.*");
  }
}
