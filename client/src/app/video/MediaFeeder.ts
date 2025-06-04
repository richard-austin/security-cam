import Hls from "hls.js";
import {Camera, Stream} from "../cameras/Camera";
import {Subscription} from "rxjs";
import {ReportingComponent} from "../reporting/reporting.component";
declare function initMSTG(): void;
// MediaStreamTrackGenerator not in lib.dom.d.ts
declare let MediaStreamTrackGenerator: any
initMSTG();  // Set up MediaStreamTrackGenerator for platforms which don't support it

export class MediaFeeder {
  isLive!: boolean;
  cam!: Camera;
  stream!: Stream;
  video!: HTMLMediaElement;
  reporting!: ReportingComponent;
  hls: any = null;
  recording: boolean = false;
  manifest: string = '';
  recordingUri: string = '';

  videoWorker!: Worker;
  audioWorker!:Worker;
  isStalled: boolean = false;
  protected _noAudio: boolean = false;

  constructor() {
  }

  init(isLive: boolean, video: HTMLMediaElement, reporting: ReportingComponent) {
    this.isLive = isLive;
    this.video = video;
    this.video.autoplay = true;
    this.video.muted = false;
    this.video.controls = !isLive;
    this.reporting = reporting;
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param cam
   * @param stream
   * @param manifest
   */
  setSource(cam: Camera, stream: Stream, manifest: string = ''): void {
    this.cam = cam;
    this.stream = stream;
    this.recording = manifest !== '';
    this.recordingUri = stream.recording.uri;

    if (this.recordingUri[this.recordingUri.length - 1] !== '/') {
      this.recordingUri += '/';
    }

    this.recordingUri += manifest;
    this.manifest = manifest;   // Save the manifest file name so it can be returned by getManifest
    this.startVideo();
  }

  /**
   * startVideo: Start the video (assumes appropriate uri and camera is set up).
   * @private
   */
  public startVideo(): void {
    if (!this.isLive) {
      if (this.cam !== undefined) {
        if (Hls.isSupported()) {
          this.hls = new Hls();
          this.hls.loadSource(this.recording ? this.recordingUri : this.stream.uri);
          this.hls.attachMedia(this.video);
        } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
          this.video.src = this.stream.uri;
        }
      }
    } else {
      this.resetTimout();
      let getUrl = window.location;
      let baseUrl = getUrl.protocol + '//' + getUrl.host + '/' + getUrl.pathname.split('/')[1];

      let url = this.stream.uri.startsWith('http:') || this.stream.uri.startsWith('https:') || this.stream.uri.startsWith('ws:')
          ?  // Absolute url
          this.stream.uri
              .replace('https', 'wss') // Change https to wss
              .replace('http', 'ws')  // or change http to ws
          :  // Relative uri
          baseUrl.substring(0, baseUrl.length - 1) // Remove trailing /
              .replace('https', 'wss') // Change https to wss
              .replace('http', 'ws')  // or change http to ws
          + this.stream.uri;

      const videoTrack = new MediaStreamTrackGenerator({kind: 'video'});
      const audioTrack = new MediaStreamTrackGenerator({kind: 'audio'});

      const videoWriter = videoTrack.writable.getWriter();
      const audioWriter = audioTrack.writable.getWriter();

      this.video.srcObject = new MediaStream([videoTrack, audioTrack]);
      this.video.onloadedmetadata = () => {
        this.video.play().then();
      }
      this.video.preload = "none";

      // let audioStream = new MediaStream([audioTrack]);
      // let ctx = new AudioContext()
      // let source = ctx.createMediaStreamSource(audioStream);
      // let dn = ctx.createDelay(2);
      // let gain = ctx.createGain();
      // gain.connect(ctx.destination);
      // gain.gain.value = 0;
      // let g2 = ctx.createGain();
      // g2.gain.value = .5;
      // source.connect(dn)
      // g2.connect(ctx.destination);
      // dn.connect(g2)
      // dn.connect(dn);
      // source.connect(gain);
      //
      // dn.delayTime.value = 0.2;
      //

      if (typeof Worker !== 'undefined') {
        // Create a new media feeder web worker
        this.videoWorker = new Worker(new URL('./video-feeder.worker', import.meta.url));
        this.videoWorker.onmessage = async ({data, type}) => {
          if(!data.warningMessage && !data.closed) {
            this.resetTimout();
            await videoWriter.write(data);
            await videoWriter.ready;
          } else if (data.closed) {
            console.log("Websocket was closed, terminating the video worker");
            this.videoWorker.terminate();
          } else if (data.warningMessage) {
            this.reporting.warningMessage = data.warningMessage;
            this.stop();
           } else {
            this.reporting.warningMessage = "Received an unknown message from the video worker "+data;
          }
        };
        this.videoWorker.postMessage({url: url})
        if(this.stream.audio) {
          this.audioWorker = new Worker(new URL('audio-feeder.worker', import.meta.url));
          this.video.onplaying = () => {
            this.audioWorker.onmessage = async ({data, type}) => {
              if (!data.warningMessage && !data.closed) {
                if (!this.video.paused) {
                  await audioWriter.write(data);
                  await audioWriter.ready;
                }
              } else if (data.closed) {
                console.log("Terminating the audio worker");
                this.audioWorker.terminate();
              } else if (!data.warningMessage) {
               this.reporting.warningMessage = data.warningMessage;
              }
            }
          }
          this.audioWorker.postMessage({url: url + 'a'})
        }
      } else {
        this.reporting.warningMessage = "Web workers are not supported in this environment";
        // You should add a fallback so that your program still executes correctly.
      }
    }
  }

  timerHandle: any = undefined;
  messageCount = 0;

  resetTimout() {
    if (this.timerHandle !== undefined) {
      clearTimeout(this.timerHandle);
    }
    // Receive 10 messages through websocket before resetting the stalled flag
    if (this.messageCount > 10)
      this.isStalled = false;
    else
      ++this.messageCount;
    this.timerHandle = setTimeout(() => {
      this.isStalled = true;
      this.messageCount = 0;
      this.audioWorker?.terminate();
      this.videoWorker?.terminate();
      this.stop();
      this.startVideo()
    }, 2000)
  }

  streamTestInterval!: Subscription;

  stop(): void {
    this.streamTestInterval?.unsubscribe();  // Stop the stream test interval, or it will be restarted after
    // we exit the video component
    if (!this.isLive && this.hls !== null) {
      this.video.pause();
      this.hls.stopLoad();
      this.hls.detachMedia();
      this.hls.destroy();
      this.hls = null;
    } else if (this.isLive) {
      if (this.timerHandle !== undefined) {
        clearTimeout(this.timerHandle);
      }
      this.videoWorker?.postMessage({close: true})
      this.videoWorker?.terminate();
      this.audioWorker?.postMessage({close: true})
      this.audioWorker?.terminate();
      this.video.pause();
    }
  }

  mute(muted: boolean = true) {
    if(this.video !== null && this.video !== undefined)
      this.video.muted = muted;
  }

  get isMuted() {
    return this.video.muted;
  }

  get hasCam(): boolean {
    return this.cam !== null && this.cam !== undefined;
  }
  get hasStream(): boolean {
    return this.stream !== null && this.stream !== undefined;
  }
  get backchannelAudioSupported(): boolean {
    return this.hasCam && this.cam.backchannelAudioSupported
  }
  get camName(): string {
    return this.hasCam ? this.cam.name : 'NO CAMERA!!';
  }

  get streamDescr() : string {
    return this.hasStream ? this.stream.descr : "NO STREAM!!"
  }

  get camera() : Camera {
    return this.cam;
  }

  get noAudio(): boolean {
    return this._noAudio;
  }
}
