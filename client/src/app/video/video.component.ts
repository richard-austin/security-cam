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
import {VideoTransformations} from "./VideoTransformations";

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
  vt!: VideoTransformations;

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
    if (this.vt !== undefined)
      this.vt.reset();
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
    this.vt.reset();
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
    this.video.addEventListener("play",()=> {
      this.vt = new VideoTransformations(this.video, this.vcEL.nativeElement);
    });
    this.video.addEventListener('fullscreenchange', () => {
      this.vt.reset();  // Set to normal scale for if the mouse wheel was turned while full screen showing
    });
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
      this.vt.reset(true);
      $event.preventDefault();
    }
    else if ($event.button === 0)
      this.vt.mouseDown($event);
  }
}
