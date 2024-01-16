import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import {Camera, Stream} from '../cameras/Camera';
import {UtilsService} from '../shared/utils.service';
import {ReportingComponent} from "../reporting/reporting.component";
import {Subscription, timer} from "rxjs";
import {MediaFeeder} from './MediaFeeder';
import {AudioBackchannel} from './AudioBackchannel';
import {MouseWheelZoom} from "./MouseWheelZoom";

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.scss']
})
export class VideoComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('video') videoEl!: ElementRef<HTMLVideoElement>;
  @ViewChild('videoContainer') vcEL!: ElementRef<HTMLDivElement>;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @Input() isFmp4: boolean = false;
  cam!: Camera;
  stream!: Stream;
  video!: HTMLVideoElement;

  visible: boolean = false;
  videoFeeder!: MediaFeeder;
  multi: boolean = false;
  buffering_sec: number = 1.2;
  audioBackchannel!: AudioBackchannel
  mouseWheelZoom!: MouseWheelZoom;

  constructor(public utilsService: UtilsService) {
    this.videoFeeder = new MediaFeeder(this.buffering_sec)
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param cam
   * @param stream
   * @param manifest
   */
  setSource(cam: Camera, stream: Stream, manifest: string = ''): void {
    if (this.mouseWheelZoom !== undefined)
      this.mouseWheelZoom.reset();
    this.audioBackchannel.stopAudioOut(); // Ensure two way audio is off when switching streams
    this.stop();
    this.stream = stream;
    this.videoFeeder.setSource(cam, stream, manifest)
    if (cam.backchannelAudioSupported) {
      this.audioBackchannel.getMediaDevices();
    }
  }

  stop(): void {
    this.videoFeeder.stop();
  }

  setFullScreen() {
    if (this.video) {
      if (this.video.requestFullscreen)
        this.video.requestFullscreen().then(() => {
        });
      // @ts-ignore
      else if (this.video.webkitRequestFullscreen)
        // @ts-ignore
        this.video.webkitRequestFullscreen();
      // @ts-ignore
      else if (this.video.msRequestFullScreen)
        // @ts-ignore
        this.video.msRequestFullScreen();
    }
  }

  toggleMuteAudio() {
    if (this.videoFeeder)
      this.videoFeeder.mute(!this.videoFeeder.isMuted);
  }

  mute(mute: boolean = true): void {
    if (this.videoFeeder)
      this.videoFeeder.mute(mute);
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.video = this.videoEl.nativeElement;
    this.videoFeeder.init(this.isFmp4, this.video);
    this.audioBackchannel = new AudioBackchannel(this.utilsService, this.reporting, this.video);
    this.mouseWheelZoom = new MouseWheelZoom(this.video, this.vcEL.nativeElement);
   // this.cd.detectChanges();
  }

  ngOnDestroy(): void {
    this.videoFeeder.stop();
    // Calling stopAudioOut directly from ngOnDestroy leaves the backchannel in a state where no UDP output ids delivered from
    //  ffmpeg to the backchannel device. The problem does not occur when done like this
    let timerSubscription: Subscription = timer(20).subscribe(() => {
      this.audioBackchannel.stopAudioOut();
      timerSubscription.unsubscribe();
    });
  }

  resetZoom($event: MouseEvent) {
    if($event.button === 1) {
      this.mouseWheelZoom.reset(true);
      $event.preventDefault();
    }
  }
}
