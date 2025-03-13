import {cameraType} from "./camera.service";

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
    lamp_mode!: number;
    lamp_mode_flag!: number;
    lamp_timeout!: number;
    lamp_ckktime!: number;
    wdr!: string;
    sdshow!: number;
    sdstatus!: string;
    sdtotalspace!: number;
    sdfreespace!: number;

}
export class Motion {
    enabled: boolean = false;
    mask_file: string = '';  // Mask file which defines area used in motion sensing
    trigger_recording_on: string = 'none';  // The name of the camera stream on which recordings will be triggered following
                                     // Motion events on this camera stream (usually another stream on the same physical
                                     // camera).
    threshold: number = 1500;  //Threshold for declaring motion.
                             // The threshold is the number of changed pixels counted after noise filtering, masking, despeckle, and labelling.
                             // The 'threshold' option is the most important detection setting.
                             // When motion runs it compares the current image frame with the previous and counts the number
                             // of changed pixels after having processed the image with noise filtering,
                             // masking, despeckle and labeling.
                             // If more pixels than defined by 'threshold' have changed we assume that we have detected motion.
                             // Set the threshold as low as possible so that you get the motion you want detected but
                             // large enough so that you do not get detections from noise and plants moving.
                             // Note that the larger your frames are, the more pixels you have. So for large picture frame
                             // sizes you need a higher threshold. Use the -s (setup mode) command line option and/or the
                             // text_changes config file option to experiment to find the right threshold value.
                             // If you do not get small movements detected (see the mouse on the kitchen floor)
                             // lower the value. If motion detects too many birds or moving trees, increase the number.
                             // (Unless of course you are one of the many users who use Motion to bird watch!)
                             // Practical values would be from a few hundred to thousands.
}

export enum RecordingType {none='none', motionService="motionService", ftpTriggered="ftpTriggered", pullPointEventTriggered="pullPointEventTriggered"}

export class Recording
{
  enabled: boolean = false
  recording_src_url: string = "";
  uri: string = "";
  location: string = "";
}

export class Stream {
  descr: string = "";
  defaultOnMultiDisplay: boolean = false;
  selected: boolean = false;
  netcam_uri: string = "";
  uri: string = "";
  media_server_input_uri: string = "";
  audio: boolean = false;
  audio_bitrate: string="0";
  audio_encoding:string = "";
  audio_sample_rate:string = "0";

  motion: Motion = new Motion();
  video_width: number = 0;
  video_height: number = 0;
  recording: Recording = new Recording();
  rec_num: number = 0;  // Used to give a rec number for the recording URI with motion triggered recordings
  preambleFrames: number = 100;
}
export class CameraParamSpec {
  constructor(camType: cameraType, params: string, uri: string, name: string) {
    this.camType = camType;
    this.params = params;
    this.uri = uri;
    this.name = name;
  }

  camType: cameraType;
  params: string;
  uri: string;
  name: string;
}

export class AudioEncoding {
  constructor(name: string, value: string) {
    this.name = name;
    this.value = value;
  }
  name: string="";
  value: string=";"
}

export class Camera
{
    name: string = "";
    address: string="";
    cameraParamSpecs!: CameraParamSpec;
    snapshotUri: string="";
    ptzControls: boolean = false;
    recordingStream: string = "none";
    streams: Map<string, Stream> = new Map<string, Stream>();
    onvifHost: string="";
    backchannelAudioSupported: boolean = false
    rtspTransport: string = "tcp";
    useRtspAuth: boolean = false;
    retriggerWindow: number = 30;
    cred: string = "";
    recordingType: RecordingType = RecordingType.none;
    motion_detection_stream: string = 'none';
    pullPointTopic: string = 'none'
    pullPointEvents: string[] = [];

    // Set up defaults for the values we're looking for in the pull point events
    simpleItemName: string = "State"
    simpleItemPositiveValue: string = "true"
    simpleItemNegativeValue: string = "false"
}
