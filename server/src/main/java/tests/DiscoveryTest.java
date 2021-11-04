package tests;

import onvif.discovery.OnvifDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;

public class DiscoveryTest {
  private static final Logger LOG = LoggerFactory.getLogger(DiscoveryTest.class);

  public static void main(String[] args) {
    Collection<URL> urls = OnvifDiscovery.discoverOnvifURLs();
    for (URL u : urls) {
      LOG.info(u.toString());
    }
  }
}
