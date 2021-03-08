import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Camera} from "../cameras/Camera";
import {CameraService} from "../cameras/camera.service";
import {Subscription, timer} from "rxjs";

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
  multi: boolean = false;

  constructor() {
  }

  setSource(cam:Camera, recording:boolean = false):void
  {
      this.camera = cam;
      this.recording = recording;
      this.startVideo();
  }

  private startVideo():void {
    if (this.camera !== undefined) {
      if (Hls.isSupported()) {
        this.hls.loadSource(this.recording ? this.camera.recording.uri : this.camera.uri);
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

    // This prevents value changed after it was checked error
    timer(10).subscribe(() => this.startVideo());

  }

  ngOnDestroy(): void {
    this.hls.stopLoad();
  }
}
