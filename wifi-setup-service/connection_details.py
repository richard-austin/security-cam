class ConnectionDetails:

    def __init__(self, name: str, uuid: str, c_type: str, device: str):
        self.name = name
        self.uuid = uuid
        self.con_type = c_type
        self.device = device

    name: str
    uuid: str
    con_type: str
    device: str
