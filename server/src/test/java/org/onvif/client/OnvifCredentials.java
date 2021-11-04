package org.onvif.client;

public class OnvifCredentials {
  private String host; // 92.168.xx.yy, or http://host[:port]
  private String user; // admin
  private String password; // secret
  private String profile; // "MediaProfile000"  If empty, will use first profile.

  public OnvifCredentials(String host, String user, String password, String profile) {
    this.host = host;
    this.user = user;
    this.password = password;
    this.profile = profile;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public String toString() {
    return host; //  + "," + user+ "," + "****,"++ "#" + profile;
  }

  public String details() {
    return host + "," + user + "," + password + "," + profile;
  }
}
