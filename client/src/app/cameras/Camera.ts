
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
    recording: Recording = new Recording();
}
