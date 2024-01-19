import {AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, Stream} from '../cameras/Camera';
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
  camera!: Camera;
  stream!: Stream;

  constructor(private route: ActivatedRoute, public cameraSvc: CameraService, private utilsService: UtilsService, private cd: ChangeDetectorRef) {
    // Use route.paramMap to get the stream name correctly if we switch directly between live streams
    this.route.paramMap.subscribe((paramMap) => {
      let streamName: string = paramMap.get('streamName') as string;
        cameraSvc.getCameras().forEach((cam) => {
          cam.streams.forEach((stream, k) => {
            if (stream.media_server_input_uri.endsWith(streamName)) {
              this.camera = cam;
              this.stream = stream;
            }
          });
         });
    })
  }

  setupVideo() {
    this.reporting.dismiss();

    this.video.visible = false;
    this.timerHandle?.unsubscribe();
    if (this.camera !== undefined && this.stream !== undefined) {
      if (this.video !== undefined) {
        this.video.setSource(this.camera, this.stream);
        this.video.visible = true;
      }
    } else
      this.showInvalidInput();
  }


  hasPTZControls() {
    return this.camera?.ptzControls;
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
      this.setupVideo();
      this.cd.detectChanges();
  }


  ngOnDestroy(): void {
    this.video.stop();
    this.timerHandle?.unsubscribe();
    // Re-enable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(true));
  }
}
