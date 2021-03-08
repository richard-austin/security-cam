import {AfterViewInit, Component, Input, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";
import {Subscription} from "rxjs";
import {VideoComponent} from "../video/video.component";

@Component({
  selector: 'app-live-container',
  templateUrl: './live-container.component.html',
  styleUrls: ['./live-container.component.scss']
})
export class LiveContainerComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() multi:boolean = false;
  @ViewChildren(VideoComponent) videos!: QueryList<VideoComponent>;

  activeLiveUpdates!: Subscription;
  timerHandle!: Subscription;

  constructor(private cameraSvc: CameraService) {
  }

  setupVideo() {
    this.videos.forEach((video) => {
      video.multi = this.multi ? this.multi : false;
      video.visible = false;
      video.stop();
    });
    let index: number = 0;
    this.cameraSvc.getActiveLive().forEach((cam: Camera) => {
      this.timerHandle?.unsubscribe();
      if (cam !== undefined) {
        let video: VideoComponent | undefined = this.videos?.get(index++);
        if (video !== undefined) {
          video.setSource(cam);
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
