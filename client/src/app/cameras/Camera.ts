
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
    descr: string = "";
    defaultOnMultiDisplay: boolean = false;
    motionName: string = "";
    netcam_uri: string = "";
    uri: string = "";
    used_in_motion_sensing: boolean = false;
    video_width: number = 0;
    video_height: number = 0;
    mask_file: string = "";
    address: string="";
    controlUri: string="";
    recording: Recording = new Recording();
}
