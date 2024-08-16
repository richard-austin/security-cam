class WifiDetails:
    def __init__(self, in_use: bool, bssid: str, ssid: str, mode: str, channel: int, rate: str, signal: int, security: str):
        self.in_use = in_use
        self.bssid = bssid
        self.ssid = ssid
        self.mode = mode
        self. channel = channel
        self. rate = rate
        self.signal = signal
        self.security = security

    in_use: bool
    bssid: str
    ssid: str
    mode: str
    channel: int
    rate: str
    signal: int
    security: str
