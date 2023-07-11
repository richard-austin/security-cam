import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, CameraStream} from '../cameras/Camera';
import {Subscription, timer} from 'rxjs';
import {VideoComponent} from '../video/video.component';
import {IdleTimeoutStatusMessage, UtilsService} from '../shared/utils.service';
import {HttpErrorResponse} from '@angular/common/http';
import {ReportingComponent} from '../reporting/reporting.component';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-live-container',
  templateUrl: './live-container.component.html',
  styleUrls: ['./live-container.component.scss']
})
export class LiveContainerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChild(VideoComponent) video!: VideoComponent;

  timerHandle!: Subscription;
  cs!: CameraStream;

  constructor(private route: ActivatedRoute, public cameraSvc: CameraService, private utilsService: UtilsService) {
    // Use route.paramMap to get the stream name correctly if we switch directly between live streams
    this.route.paramMap.subscribe((paramMap) => {
      let streamName: string = paramMap.get('streamName') as string;
      // The timer is used to allow the (await ed) call to loadAndUpdateCameraStreams to complete if switching directly
      //  from the config-setup component having run discovery and (quite legitimately) not saved changes. Without this,
      //  getCameraStreams would pick up the data from the discovery scan rather than the proper data from cameras.json
      let timerSubscription = timer(50).subscribe(() => {
        cameraSvc.getCameraStreams().forEach((cam) => {
          if (cam.stream.media_server_input_uri.endsWith(streamName)) {
            this.cs = cam;
            this.setupVideo();
          }
        });
        timerSubscription.unsubscribe();
      });
    });
  }

  setupVideo() {
    this.reporting.dismiss();

    this.video.visible = false;
    this.timerHandle?.unsubscribe();
    if (this.cs !== undefined) {
      if (this.video !== undefined) {
        this.video.setSource(this.cs);
        this.video.visible = true;
      }
    } else
      this.showInvalidInput();
  }


  hasPTZControls() {
    return this.cs?.camera?.ptzControls;
  }

  camera(): Camera | null {
    return this.cs?.camera;
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
  }


  ngOnDestroy(): void {
    this.video.stop();
    this.timerHandle?.unsubscribe();
    // Re-enable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(true));
  }
}
