
export class CameraParams
{
    hardVersion!: string;
    infraredstat!: string;
    name_0!: string;
    name_1!: string;
    model!: string;
    name!: string;
    softVersion!: string;
    startdate!: string;
    webVersion!: string;
}

export class Recording
{
  uri: string = "";
  location: string = "";
}

export class Camera
{
    name: string = "";
    motionName: string = "";
    descr: string = "";
    defaultOnMultiDisplay: boolean = false;
    uri: string = "";
    address: string="";
    controlUri: string="";
    recording: Recording = new Recording();
}
