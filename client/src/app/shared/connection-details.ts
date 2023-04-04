export class ConnectionDetails {
  constructor(con_type:string, device: string, name: string, uuid: string) {
    this.con_type = con_type;
    this.device = device;
    this.name = name;
    this.uuid = uuid;
  }
  con_type!: string;
  device!: string;
  name!: string;
  uuid!: string;
}
