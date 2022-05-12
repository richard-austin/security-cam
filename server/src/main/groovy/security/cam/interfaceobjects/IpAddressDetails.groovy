package security.cam.interfaceobjects

class IpAddressDetails {
    IpAddressDetails(String ip, ConnectionDetails cd)
    {
        this.ip = ip
        this.cd = cd
    }

    String ip
    ConnectionDetails cd
}
