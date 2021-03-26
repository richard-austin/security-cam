import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {VideoComponent} from "../video/video.component";
import {Camera} from "../cameras/Camera";
import {CameraService, LocalMotionEvent, LocalMotionEvents} from "../cameras/camera.service";
import {Subscription, timer} from "rxjs";
import {MatSelectChange} from "@angular/material/select";
import {MotionService} from "../motion/motion.service";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";
import {ActivatedRoute} from "@angular/router";
import {MatSelect} from "@angular/material/select/select";
import {MatCheckbox} from "@angular/material/checkbox";

declare let saveAs: (blob: Blob, name?: string, type?: string) => {};

@Component({
  selector: 'app-recording-control',
  templateUrl: './recording-control.component.html',
  styleUrls: ['./recording-control.component.scss']
})
export class RecordingControlComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(VideoComponent) video!: VideoComponent;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChild('selector') selector!:MatSelect;
  timerHandle!: Subscription;
  private activeLiveUpdates!: Subscription;
  motionEvents!: LocalMotionEvent[];
  validInput: boolean = false;
  private paramEpoch: number = -1;
  camera!: Camera;
  manifest: string = "";
  visible: boolean = false;
  noVideo: boolean = false;
  confirmDelete: boolean = false;
  downloading: boolean = false;

  constructor(private route: ActivatedRoute, private cameraSvc: CameraService, private motionService: MotionService) {
  }

  returnToStart() {
    this.video.video.currentTime = 0;
  }

  /**
   * stepForward: Step forward 10 seconds from the current point in the recording.
   */
  stepForward() {
    this.video.video.currentTime += 10;
  }

  /**
   * stepBack: Step back 10 seconds from the current point in the recording.
   */
  stepBack(): void {
    this.video.video.currentTime -= 10;
  }

  /**
   * stepTo: Step to the given number of seconds into the recording
   * @param time: Seconds from recording start to go to
   */
  stepTo(time: number) {
    this.video.video.currentTime = time;
  }

  /**
   * pause: Pause playback
   */
  pause(): void {
    this.video.video.pause();
  }

  /**
   * start: Start playback if stopped or set normal playback rate.
   */
  start(): void {
    this.video.video.play();
    this.video.video.playbackRate = 1;
  }

  /**
   * fastForward: Set normal playback rate X 4
   */
  fastForward() {
    this.video.video.playbackRate = 4;
  }

  /**
   * fasterForward: Set normal playback rate X 10
   */
  fasterForward(): void {
    this.video.video.playbackRate = 10;
  }

  /**
   * checkForUrlParameters: Check for parameters on the URL. There can be motionName and epoch. If there are, and
   *                        these are valid, then set the active live to the camera with the given motion name and
   *                        which is set as the default for multi display. The epoch value is returned if the
   *                        parameters were valid, otherwise -1. The parameters are supported to enable sending
   *                        links in motion event warning emails which will go straight to the event in the recording.
   */
  checkForUrlParameters(): void {
    let motionName: string;
    let epoch: number;
    this.route.params.subscribe(params => {
      motionName = params.motionName;
      if (/^[0-9]{10}$/.test(params.epoch)) {
        epoch = parseInt(params.epoch);
        let cameras: Camera[] = this.cameraSvc.getCameras();

        if (!cameras) {
          this.cameraSvc.getCamerasConfig().subscribe((cams) => {
            for (const i in cams) {
              const c = cameras[i];
              cameras.push(c);
            }
          });
        }
        let cam: Camera | undefined = cameras.find((camera: Camera) => camera.motionName === motionName && camera.defaultOnMultiDisplay);
        if (cam) {
          this.cameraSvc.setActiveLive([cam], false);
          this.paramEpoch = epoch;
        }
      }
    });
  }

  /**
   * setupRecording: Display the recording from the camera details returned from getActiveLive. This will have either
   *                 been selected from the navbar menu, or derived from the motionName given as a URL parameter.
   *                 In the latter case, an epoch time will also be given as the point to set the video time to. The
   *                 parameters are given in the URL in email motion alerts to enable immediate referencing of the
   *                 point of interest.
   */
  setupRecording() {
    this.reporting.dismiss();
    this.visible = this.noVideo = false;

    // Check for motionName and epoch time as URL parameters, use them if present and valid
    //  These are given in the URL in email motion sensing alerts to enable you to navigate straight to
    //  the relevant part of the recording.
//    this.checkForUrlParameters();
    // Check for selected camera (recording) from the nav bar menu, or URL parameters
    let cam: Camera = this.cameraSvc.getActiveLive()[0];
    // If camera (recording) available, then load that video to the page
    if (cam !== undefined) {
      this.camera = cam;
      let video: VideoComponent | undefined = this.video;
      if (video !== undefined) {
        this.video.visible = true;  // Still hidden by enclosing div
        this.video.stop();

        // Get the motion events for this camera (by motionName)
        this.motionService.getMotionEvents(cam).subscribe((events: LocalMotionEvents) => {
            this.motionEvents = events.events;
            // Get the latest recording in the set
            let motionEvent: LocalMotionEvent | undefined = this.motionEvents && this.motionEvents.length > 0 ? this.motionEvents[this.motionEvents.length - 1] : undefined;
            if (motionEvent) {
              this.manifest = motionEvent ? motionEvent.manifest : "";
              this.selector.writeValue(motionEvent);  // Make the selector show the correct event date/time

              // Give the video object the manifest of the latest recording
              if (this.manifest) {
                this.video.setSource(cam, this.manifest);

                this.visible = true;
                this.setValidInput(true);
              } else
                this.noVideo = true;
              // If there was an epoch time in the URL parameters, shift the recording to that time
              // if(this.paramEpoch !== -1)
              //   this.getOffsetForEpoch({value: this.paramEpoch});
            }
          },
          (error) => {
            this.reporting.errorMessage = error;
          });
      }
    } else
      this.setValidInput(false);
  }

  /**
   * setValidInput: Called after checking for a valid recording for this component.
   *                Hides the camera controls and displays an error message if inputValid is false.
   */
  setValidInput(inputValid: boolean): void {
    if (!inputValid) {
      this.reporting.errorMessage = new HttpErrorResponse({
        error: "No recording has been specified",
        status: 0,
        statusText: "",
        url: undefined
      });
    }
    this.validInput = inputValid;
  }

  /**
   * <h3>showMotionEvent:</h3> Show the recording selected from the drop down selector
   * @param $event
   */
  showMotionEvent($event: MatSelectChange) {
    this.manifest = $event.value.manifest;
    this.video.setSource(this.camera, $event.value.manifest);
  }

  /**
   * deleteRecording: Delete the set of files comprising the current recording
   */
  deleteRecording() {
    this.motionService.deleteRecording(this.camera, this.manifest).subscribe(() => {
        this.reporting.successMessage = "Recording " + this.selector.value.dateTime + " deleted";
        timer(2000).subscribe(() => this.setupRecording());  // Show the new latest recording
      },
      reason => {
        this.reporting.errorMessage = reason;
      }
    )
  }

  async downloadRecording()
  {
    try {
      this.downloading = true;
      let blob: Blob = await this.motionService.downloadRecording(this.camera, this.manifest);
      saveAs(blob, this.manifest.replace('.m3u8', '.mp4'))
    }
    catch(error)
    {
      this.reporting.errorMessage = error;
    }
    this.downloading = false;
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    // Call with timer to avoid expression changed after it was checked error
    this.timerHandle = timer(100).subscribe(() => {
      this.setupRecording()
      this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => {
        this.setupRecording();
      });
    });
  }


  ngOnDestroy(): void {
    this.activeLiveUpdates?.unsubscribe();
    this.timerHandle?.unsubscribe();
  }
}
