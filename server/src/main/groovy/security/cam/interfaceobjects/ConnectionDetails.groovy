package security.cam.interfaceobjects

class ConnectionDetails {
    ConnectionDetails(String name, String uuid, String con_type, String device)
    {
        this.name = name
        this.uuid = uuid
        this.con_type = con_type
        this.device = device
    }

    String name
    String uuid
    String con_type
    String device
}
