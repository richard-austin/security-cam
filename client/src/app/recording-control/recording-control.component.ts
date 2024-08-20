import {AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {VideoComponent} from '../video/video.component';
import {Camera, Stream} from '../cameras/Camera';
import {CameraService, DateSlot, LocalMotionEvent, LocalMotionEvents} from '../cameras/camera.service';
import {timer} from 'rxjs';
import {MatSelectChange} from '@angular/material/select';
import {MotionService} from '../motion/motion.service';
import {ReportingComponent} from '../reporting/reporting.component';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {MatSelect} from '@angular/material/select';
import {UtilsService} from '../shared/utils.service';
import {MatDatepickerInputEvent} from '@angular/material/datepicker';


declare let saveAs: (blob: Blob, name?: string, type?: string) => {};

declare global {
  interface Date {
    addDays(days: number): Date;
  }
}

Date.prototype.addDays = function(days: number): Date {
  let date: Date = new Date(this.valueOf());
  date.setDate(date.getDate() + days);
  return date;
};

@Component({
  selector: 'app-recording-control',
  templateUrl: './recording-control.component.html',
  styleUrls: ['./recording-control.component.scss']
})
export class RecordingControlComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(VideoComponent) video!: VideoComponent;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChild('selector') selector!: MatSelect;
  motionEvents!: LocalMotionEvent[];
  cam!: Camera;
  stream!: Stream;

  manifest: string = '';
  visible: boolean = false;
  noVideo: boolean = false;
  confirmDelete: boolean = false;
  downloading: boolean = false;
  paused: boolean = true;
  selectedPlaybackMode: string = 'startPause';
  isGuest: boolean = true;
  dateSlots: DateSlot[] = [];
  _selectedDate!: Date | null;
  _minDate!: Date;
  _maxDate!: Date;
  initialised: boolean;

  constructor(private route: ActivatedRoute, private cameraSvc: CameraService, private motionService: MotionService, private utilsService: UtilsService, private cd: ChangeDetectorRef) {
    // route.url.subscribe((u:UrlSegment[]) => {
    // });
    this.initialised = false;
    this.route.paramMap.subscribe((paramMap) => {
      let streamName: string = paramMap.get('streamName') as string;
      this.cameraSvc.getCameras().forEach((cam) => {
        cam.streams.forEach((stream, k) => {
          if (stream.media_server_input_uri.endsWith(streamName)) {
            this.cam = cam;
            this.stream = stream;
            if (this.initialised) {
              this.setupRecording();
            }
          }
        });
      });
    });
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
   * setupRecording: Display the recording from the camera details in this.cs. This will have
   *                 been selected from the navbar menu.
   */
  setupRecording() {
    this.reporting.dismiss();
    this.visible = this.noVideo = false;

    // Check for motionName and epoch time as URL parameters, use them if present and valid
    //  These are given in the URL in email motion sensing alerts to enable you to navigate straight to
    //  the relevant part of the recording.
    //    this.checkForUrlParameters();
    // Check for selected camera (recording) from the nav bar menu, or URL parameters
    // If camera (recording) available, then load that video to the page
    if (this.cam !== undefined && this.stream !== undefined) {
      let video: VideoComponent | undefined = this.video;
      if (video !== undefined) {
        this.setUpVideoEventHandlers();

        video.visible = true;  // Still hidden by enclosing div
        video.stop();
        this.selectedPlaybackMode = 'startPause';

        // Get the motion events for this camera (by motionName)
        this.motionService.getMotionEvents(this.cam, this.stream).subscribe((events: LocalMotionEvents) => {
            this.dateSlots = this.createDateSlots(events);
            // Set to the most recent date
            if (this.dateSlots.length > 0) {
              let mostRecentDateSlot: DateSlot = this.dateSlots[this.dateSlots.length - 1];
              this._selectedDate = mostRecentDateSlot.date;
              this.setSelectorsAndVideoSource(mostRecentDateSlot.lme.events);
            } else {
              this.noVideo = true;
            }
          },
          (error) => {
            this.reporting.errorMessage = error;
          });
      }
    } else {
      this.showInvalidInput(false);
    }
  }
  hasAudio (): boolean {
    return this.stream?.audio && !this.stream.motion?.enabled;
  }

  /**
   * showInvalidInput: Called after checking for a valid recording for this component.
   *                   Shows No Recording message if inputValid is false.
   */
  showInvalidInput(inputValid: boolean): void {
    if (!inputValid) {
      this.reporting.errorMessage = new HttpErrorResponse({
        error: 'No recording has been specified',
        status: 0,
        statusText: '',
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
    this.video.setSource(this.cam, this.stream, $event.value.manifest);
  }

  /**
   * deleteRecording: Delete the set of files comprising the current recording
   */
  deleteRecording() {
    this.motionService.deleteRecording(this.stream, this.manifest).subscribe(() => {
        this.reporting.successMessage = 'Recording ' + this.selector.value.dateTime + ' deleted';
        timer(2000).subscribe(() => this.setupRecording());  // Show the new latest recording
      },
      reason => {
        this.reporting.errorMessage = reason;
      }
    );
  }

  async downloadRecording() {
    try {
      this.downloading = true;
      let blob: Blob = await this.motionService.downloadRecording(this.stream, this.manifest);
      saveAs(blob, this.manifest.replace('_.m3u8', '.mp4'));
    } catch (error: any) {
      let reader: FileReader = new FileReader();
      reader.onload = () => {
        this.reporting.errorMessage = new HttpErrorResponse({
          error: JSON.parse(reader.result as string),
          status: error.status
        });
      };
      reader.readAsText(error.error);
    }
    this.downloading = false;
  }

  private setUpVideoEventHandlers() {
    if (this?.video?.video) {
      let video: HTMLVideoElement = this.video.video;

      video.onpause = () => {
        this.paused = true;
      };

      video.onplay = () => {
        this.paused = false;
      };
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
      this._minDate = ds.date;
      localMotionEvents.forEach((lme) => {
        if (lme.dateTime.substring(0, 6) === date) {
          ds.lme.events.push(lme);
        } else {
          ds = new DateSlot();
          result.push(ds);
          ds.date = new Date(Date.parse(lme.dateTime));
          date = lme.dateTime.substring(0, 6);
          ds.lme.events.push(lme);
        }
      });
      this._maxDate = ds.date;
    }
    return result;
  }

  dateFilter = (d: Date | null): boolean => {
    let ds: DateSlot | undefined = this.dateSlots.find((ds) => {
      return ds.date.getDate() === d?.getDate() && ds.date.getMonth() == d.getMonth() && ds.date.getFullYear() === d.getFullYear();
    });
    return ds !== undefined;
  };

  setDateSlot($event: MatDatepickerInputEvent<Date, Date | null>) {
    this._selectedDate = $event.value;
    if (this._selectedDate !== null) {
      let ds: DateSlot | undefined = this.dateSlots.find((ds) => {
        return this._selectedDate?.getDate() === ds.date.getDate() &&
          this._selectedDate?.getMonth() == ds.date.getMonth() &&
          this._selectedDate.getFullYear() === ds.date.getFullYear();
      });
      if (ds !== undefined && ds?.lme.events !== null) {
        this.setSelectorsAndVideoSource(ds.lme.events);
      }
    }
  }

  /**
   * selectedDate: Accessor returning type Date & string to satisfy [value] on the input element on the date picker.
   */
  get selectedDate(): Date & string {
    return this._selectedDate as Date & string;
  }

  /**
   * minDate: Accessor returning type Date & string to satisfy [min] on the input element on the date picker.
   */
  get minDate(): Date & string {
    return this._minDate as Date & string;
  }

  /**
   * maxDate: Accessor returning type Date & string to satisfy [max] on the input element on the date picker.
   */
  get maxDate(): Date & string {
    return this._maxDate as Date & string;
  }

  setSelectorsAndVideoSource(events: LocalMotionEvent[]): void {
    this.motionEvents = events;
    // Get the latest recording in the set
    let motionEvent: LocalMotionEvent | undefined = this.motionEvents && this.motionEvents.length > 0 ? this.motionEvents[this.motionEvents.length - 1] : undefined;

    this.manifest = motionEvent ? motionEvent.manifest : '';
    if (motionEvent) {
      this.selector.writeValue(motionEvent);  // Make the selector show the correct event date/time

      // Give the video object the manifest of the latest recording
      if (this.manifest) {
        this.video.setSource(this.cam, this.stream, this.manifest);

        this.visible = true;
        this.showInvalidInput(true);
      }
    }
  }

  ngOnInit(): void {
    // this.route.queryParams.subscribe(queryParams => {
    //   let x = queryParams;
    //   // do something with the query params
    // });
    //
    // this.route.params.subscribe(routeParams => {
    //   routeParams.forEach((param: any) => {
    //     let x = param;
    //   })
    //   this.setupRecording();
    // });
  }

  ngAfterViewInit(): void {
    this.isGuest = this.utilsService.isGuestAccount;
    this.initialised = true;
    this.setupRecording();
    this.cd.detectChanges();
    this.video.video.controls = false;  // Turn off controls to prevent full screen on reorientation to landscape
                                        // Also in some browsers (Firefox on Android), having the controls enabled
                                        // prevents pan and pinch zoom from working.
  }

  ngOnDestroy(): void {
    this.video.video.controls = true; // Enable controls again
  }

  toggleMuteAudio() {
    if(this.video !== undefined && this.video !== null)
      this.video.videoFeeder.mute(!this.video.videoFeeder.isMuted);
  }

  isMuted() : boolean {
    let retVal = false;
    if(this.video !== undefined && this.video !== null)
      retVal = this.video.videoFeeder.isMuted;
    return retVal;
  }
}
