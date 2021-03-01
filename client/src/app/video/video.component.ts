import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Camera, Uri, uriType} from "../cameras/Camera";
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

  private uri!:Uri;
  name!:string;

  video!: HTMLVideoElement;
  hls = new Hls();
  visible: boolean = false;

  constructor() {
  }

  setSource(uri:Uri, name:string):void
  {
      this.uri = uri;
      this.name = name;

      this.startVideo();
  }

  private startVideo():void {
    if (this.uri !== undefined) {
      if (Hls.isSupported()) {
        this.hls.loadSource(this.uri.uri);
        this.hls.attachMedia(this.video);

        //hls.on(Hls.Events.MANIFEST_PARSED, this.video.play());
        // this.video.play();
      } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
        this.video.src = this.uri.uri;
      }
    }
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

  stop() {
    this.hls.stopLoad();
  }

  ngOnDestroy(): void {
    this.hls.stopLoad();
  }
}
