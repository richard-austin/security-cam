export class WifiDetails {
  constructor(in_use: boolean,
              bssid: string,
              ssid: string,
              mode: string,
              channel: string,
              rate: string,
              signal: string,
              security: string) {
    this.in_use = in_use;
    this.bssid = bssid;
    this.ssid = ssid;
    this.mode = mode;
    this.channel = channel;
    this.rate = rate;
    this.signal = signal;
    this.security = security;
  }

  in_use: boolean;
  bssid: string;
  ssid: string;
  mode: string;
  channel: string;
  rate: string;
  signal: string;
  security: string;
}
