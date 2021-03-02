import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from "../cameras/camera.service";
import {Camera, Uri, uriType} from "../cameras/Camera";
import {MatCheckboxChange} from "@angular/material/checkbox";
import {LiveContainerComponent} from "../live-container/live-container.component";

class SelectableUri extends Uri {
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

  constructor(private cameraSvc: CameraService) {
    this.cameraSvc.setActiveLive([]);
  }

  streams: Stream[] = [];

  /**
   * setUpCameraDetails: Set up the available streams/cameras for selection by the check boxes
   */
  setUpCameraDetails(): void {
    let cams: Camera[] = [];

    this.cameraSvc.getCamerasConfig().subscribe(cameras => {
      // Build up a cameras array which excludes the addition guff which comes from
      // having the cameras set up configured in application.yml
      for (const i in cameras) {
        const c = cameras[i];
        cams.push(c);
      }
      cams?.forEach((c: Camera) => {
        let stream: Stream = new Stream();
        stream.name = c.name;

        c?.uris.forEach((u: Uri) => {
          let su:SelectableUri = u as SelectableUri;
          // Select the low resolution streams by default
          if(su.type === uriType.lo)
            su.selected = true;

          stream.uris.push(su);
        });

        this.streams.push(stream);
      });
      this.showSelected();
    });
  }

  /**
   * showSelected: Display the currently selected streams..
   */
  showSelected(): void {
    let uris: Uri[] = [];
    this.streams.forEach((s: Stream) => {
      s.uris.forEach((uri: SelectableUri) => {
        if (uri.selected)
          uris.push(uri);
      })
    });
    this.cameraSvc.setActiveLive(uris);
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
  }

  ngAfterViewInit(): void {
    this.setUpCameraDetails();
  }

  ngOnDestroy(): void {
    //   this.liveContainer?.videos.forEach((video:VideoComponent) => video.stop());
  }
}
