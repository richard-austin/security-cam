import { ConnectionDetails } from "./connection-details";

export class IPDetails {
  constructor(ip: string, cd: ConnectionDetails){
    this.ip = ip;
    this.cd = cd;
  }

  ip!: string;
  cd!: ConnectionDetails;
}
