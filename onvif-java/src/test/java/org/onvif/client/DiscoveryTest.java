package org.onvif.client;

import de.onvif.discovery.OnvifDiscovery;
import java.net.URL;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryTest {
  private static final Logger LOG = LoggerFactory.getLogger(DiscoveryTest.class);

  public static void main(String[] args) {
    Collection<URL> urls = OnvifDiscovery.discoverOnvifURLs();
    for (URL u : urls) {
      LOG.info(u.toString());
    }
  }
}
