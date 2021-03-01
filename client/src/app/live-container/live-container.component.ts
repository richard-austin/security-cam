import {AfterViewInit, Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
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
  @ViewChildren(VideoComponent) videos!: QueryList<VideoComponent>;

  activeLiveUpdates!: Subscription;
  timerHandle!: Subscription;

  constructor(private cameraSvc: CameraService) {
  }

  setupVideo() {

    this.videos.forEach((video) => {
      video.visible = false;
      video.stop();
    });
    let index: number = 0;
    this.cameraSvc.getActiveLive().forEach((uri: Uri) => {
      this.timerHandle?.unsubscribe();

      let cam: Camera | undefined = this.cameraSvc.cameraForUri(uri)
      if (uri !== undefined) {
        let video: VideoComponent | undefined = this.videos?.get(index++);
        if (video !== undefined) {
          video.setSource(uri, cam?.name + (uri.type === uriType.hd ? " (HD)" : " (Low Res)"));
          video.visible = true;
        }
      }
    });
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
