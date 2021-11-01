import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from "../cameras/camera.service";
import {Camera, CameraStream, Stream} from "../cameras/Camera";
import {MatCheckboxChange} from "@angular/material/checkbox";
import {ReportingComponent} from "../reporting/reporting.component";

@Component({
  selector: 'app-multi-cam-view',
  templateUrl: './multi-cam-view.component.html',
  styleUrls: ['./multi-cam-view.component.scss']
})
export class MultiCamViewComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild(ReportingComponent) errorReporting!: ReportingComponent;

  constructor(private cameraSvc: CameraService) {
    this.cameraSvc.setActiveLive([]);
  }

  cameras: Map<string, Camera> = new Map<string, Camera>();

  /**
   * setUpCameraDetails: Set up the available streams/cameras for selection by the check boxes
   */
  setUpCameraDetails(): void {
    this.cameraSvc.loadCameras().subscribe(cameras => {
        this.cameras = cameras;
        this.showSelected();
      },
      reason => this.errorReporting.errorMessage = reason);
  }

  /**
   * showSelected: Display the currently selected streams..
   */
  showSelected(): void {
    let cams: CameraStream[] = [];

    this.cameras.forEach((c:Camera) => {
      c.streams.forEach((stream) => {
        if (stream.selected) {
          let cs: CameraStream = new CameraStream();
          cs.camera = c;
          cs.stream = stream;
          cams.push(cs);
        }
      })
    })
    this.cameraSvc.setActiveLive(cams);
  }

  /**
   * updateCameras Respond to check box clicks to select/unselect streams. This also ensures
   *               that only one stream (HD or Low Res) can be selected at one time
   * @param $event: MatCheckbox change event including selected attribute
   * @param camera: The current camera the stream is on
   * @param stream: The stream on which the selection is being made
   */
  updateCameras($event: MatCheckboxChange, camera: Camera, stream: Stream) {
    // Ensure all other streams on this camera are deselected, only one is to be selected
    camera.streams.forEach((stream:Stream) => {
      stream.selected = false;
    })

    // Now select this one. If the checkbox was clicked when checked, make sure it doesn't go unchecked (leaving all unchecked)
    $event.source.checked = stream.selected = true;
  }

  /**
   * updateDisplay: Click handler for the Update button, changes the display to selected video streams
   */
  updateDisplay() {
    this.showSelected();
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.setUpCameraDetails();
  }

  ngOnDestroy(): void {
  }
}
