import {AfterViewInit, Component, Input, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";
import {Subscription} from "rxjs";
import {VideoComponent} from "../video/video.component";
import {IdleTimeoutStatusMessage, UtilsService} from "../shared/utils.service";
import {HttpErrorResponse} from "@angular/common/http";
import {ReportingComponent} from "../reporting/reporting.component";

@Component({
  selector: 'app-live-container',
  templateUrl: './live-container.component.html',
  styleUrls: ['./live-container.component.scss']
})
export class LiveContainerComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() multi: boolean = false;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChildren(VideoComponent) videos!: QueryList<VideoComponent>;

  activeLiveUpdates!: Subscription;
  timerHandle!: Subscription;

  constructor(private cameraSvc: CameraService, private utilsService: UtilsService) {
  }

  setupVideo() {
    this.reporting.dismiss();
    this.videos.forEach((video) => {
      video.multi = this.multi ? this.multi : false;
      video.visible = false;
      video.stop();
    });
    let index: number = 0;
    if (this.cameraSvc.getActiveLive().length > 0) {
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
    } else
      this.showInvalidInput();
  }

  /**
   * showInvalidInput: Called after checking for a valid recording for this component.
   *                   Show "No camera has been specified" error message.
   */
  showInvalidInput(): void {
    this.reporting.errorMessage = new HttpErrorResponse({
      error: "No camera has been specified",
      status: 0,
      statusText: "",
      url: undefined
    });
  }


  ngOnInit(): void {
    // Disable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(false));
  }

  ngAfterViewInit(): void {
    this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setupVideo());
    this.setupVideo();
  }

  ngOnDestroy(): void {
    this.activeLiveUpdates?.unsubscribe();
    this.timerHandle?.unsubscribe();
    // Re-enable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(true));
  }
}
