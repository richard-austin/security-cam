import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Camera} from "../cameras/Camera";
import {timer} from "rxjs";
import {UserIdleService} from "angular-user-idle";

declare let Hls: any;

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.scss']
})
export class VideoComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('video') videoEl!: ElementRef<HTMLVideoElement>

  camera!:Camera;
  video!: HTMLVideoElement;
  hls = new Hls();
  visible: boolean = false;
  recording: boolean = false;
  recordingUri: string = "";
  manifest: string = "";
  multi: boolean = false;
  private isFullscreenNow: boolean = false;

  constructor(private userIdle: UserIdleService) {
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param cam: The camera
   * @param manifest: The manifest file
   */
  setSource(cam:Camera, manifest:string=""):void
  {
      this.camera = cam;
      this.recording = manifest !== "";
      this.recordingUri = cam.recording.uri;

      if(this.recordingUri[this.recordingUri.length-1] !== '/')
        this.recordingUri+='/';

      this.recordingUri+=manifest;
      this.manifest = manifest;   // Save the manifest file name so it can be returned by getManifest
      this.startVideo();
  }

  /**
   * Get the currently selected recording manifest file
   */
  getManifest():string
  {
      return this.manifest;
  }

  /**
   * startVideo: Start the video (assumes appropriate uri and camera is set up).
   * @private
   */
  private startVideo():void {
    if (this.camera !== undefined) {
      if (Hls.isSupported()) {
        this.hls.loadSource(this.recording ? this.recordingUri : this.camera.uri);
        this.hls.attachMedia(this.video);

        //hls.on(Hls.Events.MANIFEST_PARSED, this.video.play());
        // this.video.play();
      } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
        this.video.src = this.camera.uri;
      }
    }
  }

  stop() {
    this.hls.stopLoad();
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {

    this.video = this.videoEl.nativeElement;
    this.video.autoplay = true;
    this.video.muted = true;
    this.video.controls = true;

    // Stop the idle timeout if the video is being viewed full screen
    this.video.addEventListener('webkitfullscreenchange', () => {
      this.isFullscreenNow = document.fullscreenElement !== null
      if(this.isFullscreenNow)
        this.userIdle.stopWatching();
      else
        this.userIdle.startWatching();
    });

    // This prevents value changed after it was checked error
    timer(10).subscribe(() => this.startVideo());

  }

  ngOnDestroy(): void {
    this.hls.stopLoad();

    // Ensure idle timeout is started again when we leave this. It should never be true here
    //  as we need to come out of full screen mode to exit the component.
    if(this.isFullscreenNow)
      this.userIdle.startWatching();
  }
}
