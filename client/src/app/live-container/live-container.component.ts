import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera} from '../cameras/Camera';
import {Subscription} from 'rxjs';
import {VideoComponent} from '../video/video.component';
import {IdleTimeoutStatusMessage, UtilsService} from '../shared/utils.service';
import {HttpErrorResponse} from '@angular/common/http';
import {ReportingComponent} from '../reporting/reporting.component';

@Component({
  selector: 'app-live-container',
  templateUrl: './live-container.component.html',
  styleUrls: ['./live-container.component.scss']
})
export class LiveContainerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChild(VideoComponent) video!: VideoComponent;

  activeLiveUpdates!: Subscription | undefined;
  timerHandle!: Subscription;

  constructor(public cameraSvc: CameraService, private utilsService: UtilsService) {
  }

  setupVideo() {
    this.reporting.dismiss();

    this.video.visible = false;
    let cam = this.cameraSvc.getActiveLive()[0];
    this.timerHandle?.unsubscribe();
    if (cam !== undefined) {
      if (this.video !== undefined) {
        this.video.setSource(cam);
        this.video.visible = true;
      }
    }
    else
      this.showInvalidInput();
  }


  hasPTZControls() {
    return this.cameraSvc.getActiveLive()[0]?.camera?.ptzControls;
  }

  camera(): Camera | null {
    return this.cameraSvc.getActiveLive()[0]?.camera;
  }

  /**
   * showInvalidInput: Called after checking for a valid recording for this component.
   *                   Show "No camera has been specified" error message.
   */
  showInvalidInput(): void {
    this.reporting.errorMessage = new HttpErrorResponse({
      error: 'No camera has been specified',
      status: 0,
      statusText: '',
      url: undefined
    });
  }


  ngOnInit(): void {
    // Disable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(false));
  }

  ngAfterViewInit(): void {
    if(this.activeLiveUpdates === undefined) {
      this.setupVideo();
      this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setupVideo());
    }
  }

  ngOnDestroy(): void {
    this.video.stop();
    this.activeLiveUpdates?.unsubscribe();
    this.activeLiveUpdates = undefined;
    this.timerHandle?.unsubscribe();
    // Re-enable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(true));
  }
}
