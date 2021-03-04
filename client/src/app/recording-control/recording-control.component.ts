import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {VideoComponent} from "../video/video.component";
import {Camera} from "../cameras/Camera";
import {CameraService, LocalMotionEvents, MotionEvents} from "../cameras/camera.service";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-recording-control',
  templateUrl: './recording-control.component.html',
  styleUrls: ['./recording-control.component.scss']
})
export class RecordingControlComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild(VideoComponent) video!: VideoComponent;

  timerHandle!: Subscription;
  private activeLiveUpdates!: Subscription;
  motionEvents!: LocalMotionEvents;

  constructor(private cameraSvc: CameraService) {
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

    this.cameraSvc.getActiveLive().forEach((cam: Camera) => {
      this.timerHandle?.unsubscribe();

      if (cam !== undefined) {
        let video: VideoComponent | undefined = this.video;
        if (video !== undefined) {
          video.setSource(cam, true);
          video.visible = true;
        }

        this.cameraSvc.getMotionEvents(cam).subscribe((events: LocalMotionEvents) => {
            this.motionEvents = events;
          },
          (error) => {
            // Error handling
          });
      }
    });
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setupRecording());
    this.setupRecording();
  }

  ngOnDestroy(): void {
    this.activeLiveUpdates?.unsubscribe();
    this.timerHandle?.unsubscribe();
  }

}
