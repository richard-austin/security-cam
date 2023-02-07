import {AfterViewInit, Component, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, CameraStream, Stream} from '../cameras/Camera';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {ReportingComponent} from '../reporting/reporting.component';
import {VideoComponent} from '../video/video.component';
import {HttpErrorResponse} from '@angular/common/http';
import {timer} from 'rxjs';

@Component({
  selector: 'app-multi-cam-view',
  templateUrl: './multi-cam-view.component.html',
  styleUrls: ['./multi-cam-view.component.scss']
})
export class MultiCamViewComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChildren(VideoComponent) videos!: QueryList<VideoComponent>;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  constructor(private cameraSvc: CameraService) {
  }

  cameras: Map<string, Camera> = new Map<string, Camera>();

  setupVideo() {
    this.reporting.dismiss();
    this.videos.forEach((video) => {
      video.visible = false;
      video.multi = true;
      video.stop();
    });
    let index: number = 0;
    if (this.cams.length > 0) {
      this.cams.forEach((cs) => {
        let video: VideoComponent | undefined = this.videos?.get(index++);
        if (video !== undefined) {
          video.setSource(cs);
          video.visible = true;
        }
      });
    } else {
      this.showInvalidInput();
    }
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

  /**
   * setUpCameraDetails: Set up the available streams/cameras for selection by the check boxes
   */
  setUpCameraDetails(): void {
    this.cameraSvc.loadCameras().subscribe(cameras => {
        this.cameras = cameras;
        this.showSelected();
        timer(100).subscribe(() => this.setupVideo());
      },
      reason => this.reporting.errorMessage = reason);
  }

  cams: CameraStream[] = [];

  /**
   * showSelected: Display the currently selected streams..
   */
  showSelected(): void {
    this.cams = [];
    this.cameras.forEach((c: Camera) => {
      c.streams.forEach((stream) => {
        if (stream.selected) {
          let cs: CameraStream = new CameraStream();
          cs.camera = c;
          cs.stream = stream;
          this.cams.push(cs);
        }
      });
    });
    this.setupVideo();
  }

  /**
   * updateCameras Respond to check box clicks to select/unselect streams. This also ensures
   *               that only one stream (HD or Low Res) can be selected at one time
   * @param $event: MatCheckbox change event including selected attribute
   * @param camera: The current camera the stream is on
   * @param stream: The stream on which the selection is being made
   */
  updateCameras($event: MatCheckboxChange, camera: Camera, stream: Stream) {
    if(stream.selected) {
      // If clicking on an already checked check box, ensure it remains checked and exit without updating the vids.
      $event.source.checked = true;
      return;
    }

    // Ensure all other streams on this camera are deselected, only one is to be selected
    camera.streams.forEach((stream: Stream) => {
      stream.selected = false;
    });

    // Now select this one. If the checkbox was clicked when checked, make sure it doesn't go unchecked (leaving all unchecked)
    $event.source.checked = stream.selected = true;
    timer(10).subscribe(() => this.showSelected());
  }

  ngOnInit(): void {
    this.setUpCameraDetails();
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }
}
