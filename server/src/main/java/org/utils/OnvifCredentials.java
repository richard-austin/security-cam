package org.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;

public class OnvifCredentials {
  private String onvifUrl;
  private String user; // admin
  private String password; // secret
  private String profile; // "MediaProfile000"  If empty, will use first profile.

  public OnvifCredentials(String onvifUrl, String user, String password, String profile) {
    this.onvifUrl = onvifUrl;
    this.user = user;
    this.password = password;
    this.profile = profile;
  }

  public String getOnvifUrl() {
    return onvifUrl;
  }

  public String getHost() throws MalformedURLException {
    final URL url = new URL(this.onvifUrl);
    return url.getHost()+ ":" + url.getPort();
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public String getProfile() {
    return profile;
  }

//  public String toString() {
//    return host; //  + "," + user+ "," + "****,"++ "#" + profile;
//  }
//
//  public String details() {
//    return host + "," + user + "," + password + "," + profile;
//  }
}
