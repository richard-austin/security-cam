import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraStream} from '../cameras/Camera';
import {timer} from 'rxjs';


declare let Hls: any;
declare let mpegts: any;

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.scss']
})
export class VideoComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('video') videoEl!: ElementRef<HTMLVideoElement>;
  @Input() isFlv: boolean = false;
  camstream!: CameraStream;
  video!: HTMLVideoElement;
  hls: any = null;
  flvPlayer: any = null;
  visible: boolean = false;
  recording: boolean = false;
  recordingUri: string = '';
  manifest: string = '';
  multi: boolean = false;

//  private isFullscreenNow: boolean = false;

  constructor() {
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param camStream: The camera
   * @param manifest: The manifest file
   */
  setSource(camStream: CameraStream, manifest: string = ''): void {
    this.stop();
    this.camstream = camStream;
    this.recording = manifest !== '';
    this.recordingUri = camStream.stream.recording.uri;

    if (this.recordingUri[this.recordingUri.length - 1] !== '/') {
      this.recordingUri += '/';
    }

    this.recordingUri += manifest;
    this.manifest = manifest;   // Save the manifest file name so it can be returned by getManifest
    this.startVideo();
  }

  /**
   * Get the currently selected recording manifest file
   */
  getManifest(): string {
    return this.manifest;
  }

  /**
   * startVideo: Start the video (assumes appropriate uri and camera is set up).
   * @private
   */
  private startVideo(): void {
    if (!this.isFlv) {
      if (this.camstream !== undefined) {
        if (Hls.isSupported()) {
          this.hls = new Hls();
          this.hls.loadSource(this.recording ? this.recordingUri : this.camstream.stream.uri);
          this.hls.attachMedia(this.video);

          //hls.on(Hls.Events.MANIFEST_PARSED, this.video.play());
          // this.video.play();
        } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
          this.video.src = this.camstream.stream.uri;
        }
      }
    } else {
      if (this.camstream !== undefined && mpegts.isSupported()) {
        this.stop();
        let getUrl = window.location;
        let baseUrl = getUrl.protocol + '//' + getUrl.host + '/' + getUrl.pathname.split('/')[1];

        const hasAudio: boolean = this.camstream.stream.audio_bitrate != 0;

        this.flvPlayer = mpegts.createPlayer({
            type: 'mse',
            isLive: true,
            url: this.camstream.stream.uri.startsWith('http:') || this.camstream.stream.uri.startsWith('https:')
              ?  // Absolute url
              this.camstream.stream.uri
                .replace('https', 'wss') // Change https to wss
                .replace('http', 'ws')  // or change http to ws
              :  // Relative uri
              baseUrl.substring(0, baseUrl.length - 1) // Remove trailing /
                .replace('https', 'wss') // Change https to wss
                .replace('http', 'ws')  // or change http to ws
              + this.camstream.stream.uri
          },
          {
            liveBufferLatencyChasing: true,
            liveBufferLatencyMaxLatency: hasAudio ? 1.5 : 1.05,
            liveBufferLatencyMinRemain: hasAudio ? 0.5 : 0.4,
            enableStashBuffer: false,
            enableWorker: true,
          });
//        let featureList = mpegts.getFeatureList();
        this.flvPlayer.attachMediaElement(this.video);
        this.flvPlayer.load();
        this.flvPlayer.play();
      }
    }
  }

  stop() {
    if (!this.isFlv && this.hls !== null) {
      this.video.pause();
      this.hls.stopLoad();
      this.hls.detachMedia();
      this.hls.destroy();
      this.hls = null;
    } else if (this.flvPlayer !== null) {
      this.video.pause();
      this.flvPlayer.pause();
      this.flvPlayer.unload();
      this.flvPlayer.detachMediaElement();
      this.flvPlayer.destroy();
      this.flvPlayer = null;
    }
  }

  // private fullScreenListener:() =>void = ():void => {
  //   this.isFullscreenNow = document.fullscreenElement !== null
  //   if(this.isFullscreenNow)
  //     this.userIdle.stopWatching();
  //   else
  //     this.userIdle.startWatching();
  // };
  //
  ngOnInit(): void {
  }

  ngAfterViewInit(): void {

    this.video = this.videoEl.nativeElement;
    this.video.autoplay = true;
    this.video.muted = true;
    this.video.controls = true;


    // Stop the idle timeout if the video is being viewed full screen
    // this.video.addEventListener('fullscreenchange', this.fullScreenListener);
    // this.video.addEventListener('webkitfullscreenchange', this.fullScreenListener);

    // This prevents value changed after it was checked error
    timer(10).subscribe(() => this.startVideo());
  }

  ngOnDestroy(): void {
    this.stop();
    // // Ensure idle timeout is started again when we leave this. It should never be true here
    // //  as we need to come out of full screen mode to exit the component.
    // if(this.isFullscreenNow)
    //   this.userIdle.startWatching();

    // this.video.removeEventListener('fullscreenchange', this.fullScreenListener)
    // this.video.removeEventListener('webkitfullscreenchange', this.fullScreenListener)
  }
}
