import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
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

  video!: HTMLVideoElement;
  uri!: Uri;
  hls = new Hls();
  activeLiveUpdates!: Subscription;
  name!: string;

  constructor(private cameraSvc: CameraService) {
  }

  startVideo() {
    this.uri = this.cameraSvc.getActiveLive()[0];
    this.name = "";

    for (let i in this.cameraSvc.getCameras()) {
      let cam: Camera = this.cameraSvc.getCameras()[i];
      let uri: Uri | undefined = cam.uris.find((uri) => uri === this.uri)
      if (uri === undefined) {
        uri = cam.recordings.find((uri: Uri) => uri === this.uri);
        this.name = uri !== undefined ? "Recording from " : "";
      }

      if (uri !== undefined) {
        this.name += cam.name + (uri.type === uriType.hd ? " (HD)" : " (Low Res)");
        break;
      }
    }

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

    this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.startVideo())

  }

  ngOnDestroy(): void {
    this.hls.stopLoad();
    this.activeLiveUpdates?.unsubscribe();
  }

}
