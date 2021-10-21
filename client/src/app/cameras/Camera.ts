
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
export class Motion {
  mask_file:string = '';  // Mask file which defines area used in motion sensing
  trigger_recording_on: string ='';  // The name of the camera stream on which recordings will be triggered following
                                     // Motion events on this camera stream (usually another stream on the same physical
                                     // camera).
}

export class Recording
{
  uri: string = "";
  location: string = "";
}

export class Stream {
  descr: string = "";
  defaultOnMultiDisplay: boolean = false;
  selected: boolean = false;
  netcam_uri: string = "";
  uri: string = "";
  nms_uri: string = "";
  motion!: Motion;
  video_width: number = 0;
  video_height: number = 0;
  recording: Recording = new Recording();
  absolute_num: number = 0;  // Used to give an absolute stream number for the recording URI with motion triggered recordings
}
export class Camera
{
    name: string = "";
    address: string="";
    controlUri: string="";
    streams: Map<string, Stream> = new Map<string, Stream>();
}

export class CameraStream
{
    camera: Camera = new Camera();
    stream: Stream = new Stream();
}
