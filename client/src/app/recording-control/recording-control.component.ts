import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {VideoComponent} from "../video/video.component";
import {Camera} from "../cameras/Camera";
import {CameraService, LocalMotionEvent, LocalMotionEvents, MotionEvents} from "../cameras/camera.service";
import {Subscription, timer} from "rxjs";
import {MatSelectChange} from "@angular/material/select";
import {MotionService} from "../motion/motion.service";
import {ErrorReportingComponent} from "../error-reporting/error-reporting.component";

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

  constructor(private cameraSvc: CameraService, private motionService: MotionService) {
  }

  stepForward() {
    this.video.video.currentTime += 10;
  }

  stepBack(): void {
    this.video.video.currentTime -= 10;
  }

  stepTo(time: number) {
    this.video.video.currentTime = time;
  }

  pause(): void {
    this.video.video.pause();
  }

  start(): void {
    this.video.video.play();
    this.video.video.playbackRate = 1;
  }

  fastForward() {
    this.video.video.playbackRate = 4;
  }

  fasterForward(): void {
    this.video.video.playbackRate = 10;
  }

  setupRecording() {
    this.video.visible = false;
    this.video.stop();
    this.errorReporting.dismiss();

    this.cameraSvc.getActiveLive().forEach((cam: Camera) => {
      if (cam !== undefined) {
        let video: VideoComponent | undefined = this.video;
        if (video !== undefined) {
          video.setSource(cam, true);
          video.visible = true;
        }

        this.cameraSvc.getMotionEvents(cam).subscribe((events: LocalMotionEvents) => {
            this.motionEvents = events.events;
          },
          (error) => {
            this.errorReporting.errorMessage = error;
          });
      }
    });
  }

  getOffsetForEpoch($event: MatSelectChange) {
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

  ngOnInit(): void {
    this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setupRecording());
  }

  ngAfterViewInit(): void {
    // Call with timer to avoid expression changed after it was checked error
    this.timerHandle = timer(200).subscribe(() => this.setupRecording());
  }

  ngOnDestroy(): void {
    this.activeLiveUpdates?.unsubscribe();
    this.timerHandle?.unsubscribe();
  }
}
