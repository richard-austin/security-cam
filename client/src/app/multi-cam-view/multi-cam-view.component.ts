import {AfterViewInit, Component, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, Stream} from '../cameras/Camera';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {ReportingComponent} from '../reporting/reporting.component';
import {VideoComponent} from '../video/video.component';
import {HttpErrorResponse} from '@angular/common/http';
import {timer} from 'rxjs';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {IdleTimeoutStatusMessage, UtilsService} from '../shared/utils.service';

@Component({
  selector: 'app-multi-cam-view',
  templateUrl: './multi-cam-view.component.html',
  styleUrls: ['./multi-cam-view.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
    trigger('openClose', [
      // ...
      state('open', style({
        transform: 'rotate(90deg)'
      })),
      state('closed', style({
        transform: 'rotate(0deg)'
      })),
      transition('open => closed', [
        animate('.2s')
      ]),
      transition('closed => open', [
        animate('.2s')
      ]),
    ])
  ]
})
export class MultiCamViewComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChildren(VideoComponent) videos!: QueryList<VideoComponent>;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  expandedElement!: Camera | null;
  cameraColumns = ['name', 'expand'];
  streamColumns = ['select'];

  constructor(private cameraSvc: CameraService, private utilsService: UtilsService) {
  }

  cams: Map<string, Camera> = new Map<string, Camera>();
  showStreamSelector: boolean = false;
  toggleStreamSelector() {
      this.showStreamSelector = !this.showStreamSelector;
  }

  setupVideo() {
    this.reporting.dismiss();
    this.videos.forEach((video) => {
      video.visible = false;
      video.multi = true;
      video.stop();
    });
    let index: number = 0;
    if (this.cams.size > 0) {
      this.cams.forEach((cam) => {
        cam.streams.forEach((stream, k) => {
          let video: VideoComponent | undefined = this.videos?.get(index);
          if (video !== undefined && stream.defaultOnMultiDisplay) {
            video.setSource(cam, stream);
            video.visible = true;
            ++index;
          }
        });
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
        this.cams = cameras;
//        this.showSelected();
        timer(100).subscribe(() => this.setupVideo());
      },
      reason => this.reporting.errorMessage = reason);
  }

   toggle(el: { key: string, value: Camera }) {
    this.expandedElement = this.expandedElement === el.value ? null : el.value;
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

    // Update the affected video
    timer(10).subscribe(() => {
      let index: number = 0;
      this.cams.forEach((cam) => {
        cam.streams.forEach((stream) => {
          if (camera.address === cam.address && stream.selected) {
            let video: VideoComponent | undefined = this.videos?.get(index);
            if (video !== undefined) {
              video.setSource(cam, stream);
              video.visible = true;
            }
          }
        });
        ++index;
      });
    });
  }

  ngOnInit(): void {
    this.setUpCameraDetails();
    // Disable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(false));
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    // Re-enable the user idle service
    this.utilsService.sendMessage(new IdleTimeoutStatusMessage(true));
  }
}
