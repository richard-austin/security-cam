import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {VideoComponent} from "../video/video.component";
import {CameraStream} from "../cameras/Camera";
import {CameraService, DateSlot, LocalMotionEvent, LocalMotionEvents} from "../cameras/camera.service";
import {Subscription, timer} from "rxjs";
import {MatSelectChange} from "@angular/material/select";
import {MotionService} from "../motion/motion.service";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";
import {ActivatedRoute} from "@angular/router";
import {MatSelect} from "@angular/material/select/select";
import {MatButtonToggle} from "@angular/material/button-toggle";
import {UtilsService} from '../shared/utils.service';
import {MatDatepickerInputEvent} from '@angular/material/datepicker';
import { FormControl } from '@angular/forms';

declare let saveAs: (blob: Blob, name?: string, type?: string) => {};

declare global {
  interface Date {
    addDays(days: number): Date;
  }
}

Date.prototype.addDays = function (days: number): Date {
  let date: Date = new Date(this.valueOf());
  date.setDate(date.getDate() + days);
  return date;
}

@Component({
  selector: 'app-recording-control',
  templateUrl: './recording-control.component.html',
  styleUrls: ['./recording-control.component.scss']
})
export class RecordingControlComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(VideoComponent) video!: VideoComponent;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChild('selector') selector!: MatSelect;
  @ViewChild('recordingButtonGroup') recordingButtonGroup!: ElementRef<MatButtonToggle>;
  timerHandle!: Subscription;
  private activeLiveUpdates!: Subscription;
  motionEvents!: LocalMotionEvent[];
  cs!: CameraStream;
  manifest: string = "";
  visible: boolean = false;
  noVideo: boolean = false;
  confirmDelete: boolean = false;
  downloading: boolean = false;
  paused: boolean = true;
  selectedPlaybackMode: string = "startPause";
  isGuest: boolean = true;
  dateSlots: DateSlot[] = [];
  selectedDate!: Date | null;

  constructor(private route: ActivatedRoute, private cameraSvc: CameraService, private motionService: MotionService, private utilsService: UtilsService) {
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
   * pause: Pause playback
   */
  pause(): void {
    this.video.video.pause();
  }

  private _start() {
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
    let cs: CameraStream = this.cameraSvc.getActiveLive()[0];
    // If camera (recording) available, then load that video to the page
    if (cs !== undefined) {
      this.cs = cs;
      let video: VideoComponent | undefined = this.video;
      if (video !== undefined) {
        this.setUpVideoEventHandlers();

        video.visible = true;  // Still hidden by enclosing div
        video.stop();
        this.selectedPlaybackMode = 'startPause';

        // Get the motion events for this camera (by motionName)
        this.motionService.getMotionEvents(cs).subscribe((events: LocalMotionEvents) => {
            this.dateSlots = this.createDateSlots(events);
            // Set to the most recent date
            let mostRecentDateSlot: DateSlot = this.dateSlots[this.dateSlots.length - 1];
            this.selectedDate = mostRecentDateSlot.date;
            this.setSelectorsAndVideoSource(mostRecentDateSlot.lme.events);
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
    this.video.setSource(this.cs, $event.value.manifest);
  }

  /**
   * deleteRecording: Delete the set of files comprising the current recording
   */
  deleteRecording() {
    this.motionService.deleteRecording(this.cs.stream, this.manifest).subscribe(() => {
        this.reporting.successMessage = "Recording " + this.selector.value.dateTime + " deleted";
        timer(2000).subscribe(() => this.setupRecording());  // Show the new latest recording
      },
      reason => {
        this.reporting.errorMessage = reason;
      }
    )
  }

  async downloadRecording() {
    try {
      this.downloading = true;
      let blob: Blob = await this.motionService.downloadRecording(this.cs.stream, this.manifest);
      saveAs(blob, this.manifest.replace('.m3u8', '.mp4'))
    } catch (error) {
      let reader: FileReader = new FileReader();
      reader.onload = () => {
        this.reporting.errorMessage = new HttpErrorResponse({
          error: JSON.parse(reader.result as string),
          status: error.status
        });
      }
      reader.readAsText(error.error);
    }
    this.downloading = false;
  }

  private setUpVideoEventHandlers() {
    if (this?.video?.video) {
      let video: HTMLVideoElement = this.video.video;

      video.onpause = () => {
        this.paused = true;
      }

      video.onplay = () => {
        this.paused = false;
      }
    }
  }

  /**
   * createDateSlots: Put the motion events into an array for their particular date. Return the result as an array
   *                  of these arrays, with the whole date range.
   * @param events
   */
  createDateSlots(events: LocalMotionEvents): DateSlot[] {
    const localMotionEvents: LocalMotionEvent[] = events.events;
    const result: DateSlot[] = [];

    if (localMotionEvents.length > 0) {
      let date: string = localMotionEvents[0].dateTime.substring(0, 6); // Just the day and month
      let ds: DateSlot = new DateSlot();
      result.push(ds);
      ds.date = new Date(Date.parse(localMotionEvents[0].dateTime));
      localMotionEvents.forEach((lme) => {
        if (lme.dateTime.substring(0, 6) === date)
          ds.lme.events.push(lme);
        else {
          ds = new DateSlot();
          result.push(ds);
          ds.date = new Date(Date.parse(lme.dateTime));
          date = lme.dateTime.substring(0, 6);
          ds.lme.events.push(lme)
        }
      });
    }
    return result;
  }

  dateFilter = (d: Date | null): boolean => {
    let ds: DateSlot | undefined = this.dateSlots.find((ds) => {
      return ds.date.getDate() === d?.getDate() && ds.date.getMonth() == d.getMonth() && ds.date.getFullYear() === d.getFullYear();
    })
    return ds !== undefined;
  };

  startDate(): Date {
    return this.dateSlots !== undefined && this.dateSlots.length > 0
      ? this.dateSlots[this.dateSlots.length - 1].date :
      new Date();
  }


  setDateSlot($event: MatDatepickerInputEvent<Date, Date | null>) {
    this.selectedDate = $event.value;
    if (this.selectedDate !== null) {
      let ds: DateSlot | undefined = this.dateSlots.find((ds) => {
        return this.selectedDate?.getDate() === ds.date.getDate() &&
          this.selectedDate?.getMonth() == ds.date.getMonth() &&
          this.selectedDate.getFullYear() === ds.date.getFullYear();
      })
      if (ds !== undefined && ds?.lme.events !== null) {
        this.setSelectorsAndVideoSource(ds.lme.events);
      }
    }
  }

  setSelectorsAndVideoSource(events: LocalMotionEvent[]): void {
    this.motionEvents = events;
    // Get the latest recording in the set
    let motionEvent: LocalMotionEvent | undefined = this.motionEvents && this.motionEvents.length > 0 ? this.motionEvents[this.motionEvents.length - 1] : undefined;

    this.manifest = motionEvent ? motionEvent.manifest : "";
    if (motionEvent) {
      this.selector.writeValue(motionEvent);  // Make the selector show the correct event date/time

      // Give the video object the manifest of the latest recording
      if (this.manifest) {
        this.video.setSource(this.cs, this.manifest);

        this.visible = true;
        this.showInvalidInput(true);
      }
    } else
      this.noVideo = true;
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
    this.isGuest = this.utilsService.isGuestAccount;
  }


  ngOnDestroy(): void {
    this.activeLiveUpdates?.unsubscribe();
    this.timerHandle?.unsubscribe();
  }
}
