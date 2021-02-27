import {AfterViewInit, Component, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {CameraService} from "../cameras/camera.service";
import {Camera, Uri, uriType} from "../cameras/Camera";
import {Subscription, timer} from "rxjs";
import {VideoComponent} from "../video/video.component";

@Component({
  selector: 'app-live-container',
  templateUrl: './live-container.component.html',
  styleUrls: ['./live-container.component.scss']
})
export class LiveContainerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChildren(VideoComponent) videos!:QueryList<VideoComponent>;

  activeLiveUpdates!: Subscription;
  uri!:Uri;

  timerHandle!: Subscription;

  constructor(private cameraSvc: CameraService) { }

  setupVideo() {
    this.uri = this.cameraSvc.getActiveLive()[0];
    this.videos.forEach((video) => video.visible = false);

    // Use timer to prevent expression changed after it was checked error
    this.timerHandle?.unsubscribe();
    this.timerHandle = timer(20).subscribe(() => {
        for (let i in this.cameraSvc.getCameras()) {
          let cam: Camera = this.cameraSvc.getCameras()[i];
          let uri: Uri = cam.uris.find((uri) => uri === this.uri) as Uri;
          // if (uri === undefined) {
          //   uri = cam.recordings.find((uri: Uri) => uri === this.uri) as Uri;
          //   this.name = uri !== undefined ? "Recording from " : "";
          // }

          if (uri !== undefined) {
            let index = parseInt(i);
            let video: VideoComponent | undefined = this.videos?.get(index);

            if (video !== undefined) {
              video.setSource(uri, cam.name + (uri.type === uriType.hd ? " (HD)" : " (Low Res)"));

              video.visible = true;
            }
            break;
          }
        }
      }
    );
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setupVideo());
    this.setupVideo();
  }

  ngOnDestroy(): void {
    this.activeLiveUpdates?.unsubscribe();
    this.timerHandle?.unsubscribe();
  }
}
