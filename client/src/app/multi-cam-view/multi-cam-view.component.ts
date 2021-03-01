import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from "../cameras/camera.service";
import {Camera, Uri} from "../cameras/Camera";
import {MatCheckboxChange} from "@angular/material/checkbox";
import {LiveAnnouncerDefaultOptions} from "@angular/cdk/a11y";
import {LiveContainerComponent} from "../live-container/live-container.component";
import {VideoComponent} from "../video/video.component";

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

        c.uris.forEach((u: Uri) => {
          stream.uris.push(u as SelectableUri);
        });

        this.streams.push(stream);
      });
    });
  }

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

  updateCameras($event: MatCheckboxChange, stream: Stream, uri: SelectableUri) {
    // Ensure all other uris in this stream are disabled, only one is to e enabled
    stream.uris.forEach((u: SelectableUri) => u.selected = false);

    // now select/unselect this one
    uri.selected = $event.checked;
  }

  updateDisplay() {
    this.showSelected();
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.setUpCameraDetails();
    this.showSelected();
  }

  ngOnDestroy(): void {
    //   this.liveContainer?.videos.forEach((video:VideoComponent) => video.stop());
  }
}
