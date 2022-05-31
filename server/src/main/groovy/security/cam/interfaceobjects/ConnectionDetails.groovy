package security.cam.interfaceobjects

class ConnectionDetails {
    ConnectionDetails(String name, String driver, String con_type, String device)
    {
        this.name = name
        this.driver = driver
        this.con_type = con_type
        this.device = device
    }

    String name
    String driver
    String con_type
    String device
}
