package tests;

import onvif.discovery.OnvifDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Class calls OnvifDiscovery and for each device URL found, calls TestDevice This assumes all onvif
 * devices on your network use the same username and password.
 *
 * @author Brad Lowe
 */
public class DiscoverAndTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestDevice.class);

  public static String discoverAndTest(String user, String password) {
    String sep = "\n";
    StringBuffer out = new StringBuffer();

    Collection<URL> urls = OnvifDiscovery.discoverOnvifURLs();
    for (URL u : urls) {
      out.append("Discovered URL:" + u.toString() + sep);
    }
    ArrayList<String> results = new ArrayList<>();

    int good = 0, bad = 0;

    for (URL u : urls) {

      try {
        String result = TestDevice.testCamera(u, user, password);
        LOG.info(u + "->" + result);
        good++;
        results.add(u.toString() + ":" + result);
      } catch (Throwable e) {
        bad++;
        LOG.error("error:" + u, e);
        // This is a bit of a hack. When a camera is password protected (it should be!)
        // and the password is not provided or wrong, a "Unable to Send Message" exception
        // may be thrown. This is not clear-- buried in the stack track is the real cause.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String trace = sw.getBuffer().toString();
        if (trace.contains("Unauthorized")) results.add(u + ":Unauthorized:");
        else results.add(u + ":" + trace);
      }
    }
    out.append("RESULTS: " + sep);
    for (String s : results) out.append(s + sep);
    out.append("cameras found:" + urls.size() + " good=" + good + ", bad=" + bad + sep);
    return out.toString();
  }

  public static void mainX(String[] args) {
    // get user and password.. we will ignore device host
    String user = "";
    String password = "";
    if (args.length > 0) user = args[0];
    if (args.length > 1) password = args[1];

    if (password.isEmpty()) {
      LOG.warn(
          "Warning: No password for discover and test... run with common user password as arguments");
    }
    // OnvifDevice.setVerbose(true);
    LOG.info(discoverAndTest(user, password));
  }
}
