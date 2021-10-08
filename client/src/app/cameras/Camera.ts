
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
class Motion {
  name!: string;  // Motion name
  mask_file!:string;  // Mask file which defines area used in motion sensing
  trigger_recording_on!: string;  // The name of the camera stream on which recordings will be triggered following
                                  // Motion events on this camera stream (usually another stream on the same physical
                                  // camera).
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
    netcam_uri: string = "";
    uri: string = "";
    nms_uri: string = "";
    motion!: Motion;
    video_width: number = 0;
    video_height: number = 0;
    address: string="";
    controlUri: string="";
    recording: Recording = new Recording();
}
