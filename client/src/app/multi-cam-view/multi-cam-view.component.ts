import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";
import {MatCheckboxChange} from "@angular/material/checkbox";
import {LiveContainerComponent} from "../live-container/live-container.component";
import {ReportingComponent} from "../reporting/reporting.component";
import {UserIdleService} from "angular-user-idle";

class SelectableUri extends Camera{
  selected: boolean = false;
}

export class Stream {
  uris: SelectableUri[] = [];
  name: string = "";
}

@Component({
  selector: 'app-multi-cam-view',
  templateUrl: './multi-cam-view.component.html',
  styleUrls: ['./multi-cam-view.component.scss']
})
export class MultiCamViewComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild(LiveContainerComponent) liveContainer!: LiveContainerComponent;
  @ViewChild(ReportingComponent) errorReporting!:ReportingComponent;

  constructor(private cameraSvc: CameraService, private userIdle: UserIdleService) {
    this.cameraSvc.setActiveLive([]);
  }

  streams: Stream[] = [];

  /**
   * setUpCameraDetails: Set up the available streams/cameras for selection by the check boxes
   */
  setUpCameraDetails(): void {
    this.cameraSvc.getCamerasConfig().subscribe(cameras => {
      // Build up the cameras array
      for (const i in cameras) {
        const c = cameras[i];
        // List camera names only once, with their available streams
        let stream: Stream | undefined = this.streams.find((s: Stream) => s.name === c.name);
        if (stream === undefined) {
          stream = new Stream();
          stream.name = c.name;
          this.streams.push(stream);
        }

        let su: SelectableUri = c as SelectableUri;
        su.selected = c.defaultOnMultiDisplay;
        stream.uris.push(su);

    }
      this.showSelected();
    },
      reason => this.errorReporting.errorMessage = reason);
  }

  /**
   * showSelected: Display the currently selected streams..
   */
  showSelected(): void {
    let cams: Camera[] = [];
    this.streams.forEach((s: Stream) => {
      s.uris.forEach((uri: SelectableUri) => {
        if (uri.selected)
          cams.push(uri);
      })
    });
    this.cameraSvc.setActiveLive(cams);
  }

  /**
   * updateCameras Respond to check box clicks to select/unselect streams. This also ensures
   *               that only one stream (HD or Low Res) can be selected at one time
   * @param $event: MatCheckbox change event including selected attribute
   * @param stream: The stream on which the selection is being made
   * @param uri: The specific uri on which the selection change is being made
   */
  updateCameras($event: MatCheckboxChange, stream: Stream, uri: SelectableUri) {
    // Ensure all other uris in this stream are disabled, only one is to e enabled
    stream.uris.forEach((u: SelectableUri) => u.selected = false);

    // now select/unselect this one
    uri.selected = $event.checked;
  }

  /**
   * updateDisplay: Click handler for the Update button, changes the display to selected video streams
   */
  updateDisplay() {
    this.showSelected();
  }

  ngOnInit(): void {
    this.userIdle.stopWatching();
  }

  ngAfterViewInit(): void {
    this.setUpCameraDetails();
  }

  ngOnDestroy(): void {
    this.userIdle.startWatching();
    //   this.liveContainer?.videos.forEach((video:VideoComponent) => video.stop());
  }
}
