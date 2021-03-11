import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {VideoComponent} from "../video/video.component";
import {Camera} from "../cameras/Camera";
import {CameraService, LocalMotionEvent, LocalMotionEvents, MotionEvents} from "../cameras/camera.service";
import {Subscription, timer} from "rxjs";
import {MatSelectChange} from "@angular/material/select";
import {MotionService} from "../motion/motion.service";
import {ErrorReportingComponent} from "../error-reporting/error-reporting.component";
import {HttpErrorResponse} from "@angular/common/http";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-recording-control',
  templateUrl: './recording-control.component.html',
  styleUrls: ['./recording-control.component.scss']
})
export class RecordingControlComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(VideoComponent) video!: VideoComponent;
  @ViewChild(ErrorReportingComponent) errorReporting!: ErrorReportingComponent;
  timerHandle!: Subscription;
  private activeLiveUpdates!: Subscription;
  motionEvents!: LocalMotionEvent[];
  validInput: boolean = false;
  private paramEpoch: number = -1;

  constructor(private route:ActivatedRoute, private cameraSvc: CameraService, private motionService: MotionService) {
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
   *                        which is set as the default for multi display. Te epoch value is returned if the
   *                        parameters were valid, otherwise -1. The parameters are supported to enable sending
   *                        links in motion event warning emails which will go straight to the event in the recording.
   */
  checkForUrlParameters() : void
  {
    let motionName: string;
    let epoch: number;
    this.route.params.subscribe(params => {
      motionName = params.motionName;
      if(/^[0-9]{10}$/.test(params.epoch)) {
        epoch = parseInt(params.epoch);
        let cameras:Camera[] = this.cameraSvc.getCameras();

        if(!cameras)
        {
          this.cameraSvc.getCamerasConfig().subscribe((cams) =>{
            for (const i in cams) {
              const c = cameras[i];
              cameras.push(c);
            }
          });
        }
        let cam:Camera|undefined = cameras.find((camera:Camera) => camera.motionName === motionName && camera.defaultOnMultiDisplay);
        if(cam)
        {
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
    this.video.visible = false;
    this.video.stop();
    this.errorReporting.dismiss();
    // Check for motionName and epoch time as URL parameters, use them if present and valid
    //  These are given in the URL in email motion sensing alerts to enable you to navigate straight to
    //  the relevant part of the recording.
    this.checkForUrlParameters();
    // Check for selected camera (recording) from the nav bar menu, or URL parameters
    let cam: Camera = this.cameraSvc.getActiveLive()[0];
    // If camera (recording) available, then load that video to the page
      if (cam !== undefined) {
        let video: VideoComponent | undefined = this.video;
        if (video !== undefined) {
          video.setSource(cam, true);
          video.visible = true;
          this.setValidInput(true);
        }
        // Get the motion events for this camera (by motionName)
        this.cameraSvc.getMotionEvents(cam).subscribe((events: LocalMotionEvents) => {
            this.motionEvents = events.events;
            // If there was an epoch time in the URL parameters, shift the recording to that time
            if(this.paramEpoch !== -1)
              this.getOffsetForEpoch({value: this.paramEpoch});
          },
          (error) => {
            this.errorReporting.errorMessage = error;
          });
      }
      else
        this.setValidInput(false);
  }

  /**
   * getOffsetForEpoch: From an epoch time, representing an absolute date/time, calculate the time offset into the
   *                    currently shown recording at which this date/time is shown.
   * @param $event: Either :-
   *                    An Angular Material select box change event containing the selected date/time (given as epoch)
   *                Or :-
   *
   */
  getOffsetForEpoch($event: MatSelectChange | {value: number} ) {
    this.errorReporting.dismiss();
    let epoch: any = $event.value;
    let motionName: string = this.video.camera.motionName;
    this.motionService.getTimeOffsetForEpoch(epoch, motionName).subscribe(offset => {
        this.stepTo(parseInt(offset.offset) - 10);
      },
      reason => {
        this.errorReporting.errorMessage = reason;
      });
  }

  /**
   * setValidInput: Called after checking for a valid recording for this component.
   *                Hides the camera controls and displays an error message if inputValid is false.
   */
  setValidInput(inputValid:boolean):void {
    if(!inputValid) {
      this.errorReporting.errorMessage = new HttpErrorResponse({
        error: "No recording has been specified",
        status: 0,
        statusText: "",
        url: undefined
      });
    }
    this.validInput = inputValid;
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    // Call with timer to avoid expression changed after it was checked error
    this.timerHandle = timer(2000).subscribe(() => {
      this.setupRecording()
      this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setupRecording());
    });
  }

  ngOnDestroy(): void {
    this.activeLiveUpdates?.unsubscribe();
    this.timerHandle?.unsubscribe();
  }
}
