import Hls from "hls.js";
import {Camera, Stream} from "../cameras/Camera";
import {Subscription} from "rxjs";
declare function initMSTG(): void;
// MediaStreamTrackGenerator not in lib.dom.d.ts
declare let MediaStreamTrackGenerator: any
initMSTG();  // Set up MediaStreamTrackGenerator for platforms which don't support it

export class MediaFeeder {
  isLive!: boolean;
  cam!: Camera;
  stream!: Stream;
  media!: HTMLMediaElement;
  hls: any = null;
  recording: boolean = false;
  manifest: string = '';
  recordingUri: string = '';

  videoWorker!: Worker;
  audioWorker!:Worker;
  isStalled: boolean = false;

  constructor() {
  }

  init(isfmp4: boolean, media: HTMLMediaElement) {
    this.isLive = isfmp4;
    this.media = media;
    this.media.autoplay = true;
    this.media.muted = false;
    this.media.controls = !isfmp4;
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param cam
   * @param stream
   * @param manifest
   */
  setSource(cam: Camera, stream: Stream, manifest: string = ''): void {
   // this.stop();
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
          this.hls.attachMedia(this.media);
        } else if (this.media.canPlayType('application/vnd.apple.mpegurl')) {
          this.media.src = this.stream.uri;
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

      // @ts-ignore
      const audioTrack = new window.MediaStreamTrackGenerator({kind: 'audio'});

      const videoWriter = videoTrack.writable.getWriter();
      const audioWriter = audioTrack.writable.getWriter();

      this.media.srcObject = new MediaStream([videoTrack, audioTrack])
      this.media.onloadedmetadata = () => {
        this.media.play().then();
      }
      this.media.preload = "none";

      if (typeof Worker !== 'undefined') {
        // Create a new media feeder web worker
        this.videoWorker = new Worker(new URL('./video-feeder.worker', import.meta.url));
        this.videoWorker.onmessage = async ({data, type}) => {
          if (data.closed) {
            console.log("Terminating the video worker");
            this.videoWorker.terminate();
          } else if (data.codecNotSupported) {
            console.log("Codec " + data.codec + " not supported by browser");
            this.videoWorker.terminate();
            this.audioWorker?.postMessage({close: true})
            this.audioWorker?.terminate();
          } else {
            let processStart = performance.now();
            this.resetTimout();
            await videoWriter.write(data);
            await videoWriter.ready;
          }
        };
        this.videoWorker.postMessage({url: url})
        if(this.stream.audio) {
          this.audioWorker = new Worker(new URL('audio-feeder.worker', import.meta.url));
          this.media.onplaying = () => {
            this.audioWorker.onmessage = async ({data, type}) => {
              if (data.closed) {
                console.log("Terminating the audio worker");
                this.audioWorker.terminate();
              } else if (!this.media.paused) {
                await audioWriter.write(data);
                await audioWriter.ready;
              }
            }
          }
          this.audioWorker.postMessage({url: url + 'a'})
        }
      } else {
        // Web workers are not supported in this environment.
        // You should add a fallback so that your program still executes correctly.
      }

    }
  }

  timerHandle: any = undefined;

  resetTimout() {
    this.isStalled = false;
    if (this.timerHandle !== undefined) {
      clearTimeout(this.timerHandle);
    }
    this.timerHandle = setTimeout(() => {
      this.isStalled = true;
    }, 2000)
  }

  streamTestInterval!: Subscription;

  stop(): void {
    this.streamTestInterval?.unsubscribe();  // Stop the stream test interval, or it will be restarted after
    // we exit the video component
    if (!this.isLive && this.hls !== null) {
      this.media.pause();
      this.hls.stopLoad();
      this.hls.detachMedia();
      this.hls.destroy();
      this.hls = null;
    } else if (this.isLive) {
      this.videoWorker?.postMessage({close: true})
      this.videoWorker?.terminate();
      this.audioWorker?.postMessage({close: true})
      this.audioWorker?.terminate();
      this.media.pause();
    }
  }

  mute(muted: boolean = true) {
    if(this.media !== null && this.media !== undefined)
      this.media.muted = muted;
  }

  get isMuted() {
    return this.media !== null && this.media !== undefined && this.media.muted;
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
}
