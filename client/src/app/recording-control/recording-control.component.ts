import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
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
import {MatButtonToggle} from "@angular/material/button-toggle";

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
  @ViewChild('recordingButtonGroup') recordingButtonGroup!:ElementRef<MatButtonToggle>;
  timerHandle!: Subscription;
  private activeLiveUpdates!: Subscription;
  motionEvents!: LocalMotionEvent[];
  camera!: Camera;
  manifest: string = "";
  visible: boolean = false;
  noVideo: boolean = false;
  confirmDelete: boolean = false;
  downloading: boolean = false;
  paused: boolean = true;
  selectedPlaybackMode: string ="startPause";

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

  private _start()
  {
    this?.video?.video.play();
  }

  /**
   * start: Start playback if stopped or set normal playback rate.
   */
  start(): void {
    this._start();
    this.video.video.playbackRate = 1;
  }

  /**
   * fastForward: Set normal playback rate X 4
   */
  fastForward() {
    this._start();
    this.video.video.playbackRate = 4;
  }

  /**
   * fasterForward: Set normal playback rate X 10
   */
  fasterForward(): void {
    this._start();
    this.video.video.playbackRate = 10;
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
        this.setUpVideoEventHandlers();

        video.visible = true;  // Still hidden by enclosing div
        video.stop();
        this.selectedPlaybackMode = 'startPause';

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
                this.showInvalidInput(true);
              }
            }
            else
              this.noVideo = true;
          },
          (error) => {
            this.reporting.errorMessage = error;
          });
      }
    } else
      this.showInvalidInput(false);
  }

  /**
   * showInvalidInput: Called after checking for a valid recording for this component.
   *                   Shows No Recording message if inputValid is false.
   */
  showInvalidInput(inputValid: boolean): void {
    if (!inputValid) {
      this.reporting.errorMessage = new HttpErrorResponse({
        error: "No recording has been specified",
        status: 0,
        statusText: "",
        url: undefined
      });
    }
  }

  /**
   * <h3>showMotionEvent:</h3> Show the recording selected from the drop down selector
   * @param $event
   */
  showMotionEvent($event: MatSelectChange) {
    this.manifest = $event.value.manifest;
    this.selectedPlaybackMode = 'startPause';
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

  private setUpVideoEventHandlers() {
      if(this?.video?.video)
      {
        let video:HTMLVideoElement = this.video.video;

        video.onpause = () => {
          this.paused = true;
        }

        video.onplay = () =>
        {
          this.paused = false;
        }
      }
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
