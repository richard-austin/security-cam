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
import {VideoSizing} from "./VideoSizing";
import {SharedModule} from "../shared/shared.module";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {FormsModule} from "@angular/forms";
import {AudioControlComponent} from "./audio-control/audio-control.component";
import {animate, state, style, transition, trigger} from "@angular/animations";

@Component({
    selector: 'app-video',
    templateUrl: './video.component.html',
    styleUrls: ['./video.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({width: '0px', minWidth: '0'})),
      state('expanded', style({width: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ])
    ],
    imports: [SharedModule, SharedAngularMaterialModule, FormsModule, AudioControlComponent]
})
export class VideoComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('video') videoEl!: ElementRef<HTMLVideoElement>;
  @ViewChild('videoContainer') vcEL!: ElementRef<HTMLDivElement>;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChild('videoControls') videoControlsEL!: ElementRef<HTMLDivElement>;
  @Input() isLive: boolean = false;
  cam!: Camera;
  stream!: Stream;
  video!: HTMLVideoElement;
  audio!: HTMLAudioElement;

  volume: number = 0.4;

  visible: boolean = false;
  mediaFeeder!: MediaFeeder;
  multi: boolean = false;
  audioBackchannel!: AudioBackchannel
  vt!: VideoTransformations;
  currentTime: string = "";
  totalTime: string = "";
  sizing!: VideoSizing;
  showAudioControls: boolean = false;

  constructor(public utilsService: UtilsService) {
    this.mediaFeeder = new MediaFeeder();
    this.audio = new Audio();
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
    this.audioBackchannel.stopAudioOut().then(r => {}); // Ensure two way audio is off when switching streams
    this.mediaFeeder.stop();
    this.stream = stream;
    this.mediaFeeder.setSource(cam, stream, manifest)
    if (cam.backchannelAudioSupported) {
      this.audioBackchannel.getMediaDevices();
    }
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

  toggleMuteAudio(muted: boolean) {
    if (this.mediaFeeder)
      this.mediaFeeder.mute(muted);
    this.volume = this.mediaFeeder.isMuted ? 0 : this.audio.volume;
  }

  mute(mute: boolean = true): void {
    if (this.mediaFeeder)
      this.mediaFeeder.mute(mute);
  }

  setVolume(volume: number) {
    this.volume = volume;
    this.audio.volume = this.volume;
  }

  toggleShowAudioControls() {
    this.showAudioControls = !this.showAudioControls;
  }

  setSize(size: number, isRecording: boolean = false): void {
    this.sizing.setup(size, isRecording)
  }
  changeSize(size: number) {
    this.sizing.changeSize(size);
  }

  reset($event: MouseEvent) {
    if ($event.button === 1) {
      this.vt.reset(true);
      $event.preventDefault();
    } else if ($event.button === 0)
      this.vt.mouseDown($event);
  }

  orientationChangeHandler = (ev: Event) => {
    if (ev.currentTarget instanceof ScreenOrientation) {
      if (!this.multi) {
        // Set up VideoTransformations again to take account of viewport dimension changes
        this.vt = new VideoTransformations(this.video, this.vcEL.nativeElement);
        this.vt.reset();  // Clear any pan/zoom
      }
    }
  }

  ngOnInit(): void {
  }

  clickHandler =  (ev: Event) => {
    const inVideoControlDialogue = ev.composedPath().includes(this.videoControlsEL.nativeElement);
    if (!inVideoControlDialogue)
      this.showAudioControls = false;
  };
  ngAfterViewInit(): void {
    document.addEventListener('click', this.clickHandler);
    this.video = this.videoEl.nativeElement;
    this.volume = this.audio.volume = 0.4;
    this.mediaFeeder.init(this.isLive, this.video, this.audio, this.reporting);
    this.audioBackchannel = new AudioBackchannel(this.utilsService, this.reporting, this.video);
    this.vt = new VideoTransformations(this.video, this.vcEL.nativeElement);
    this.video.addEventListener('fullscreenchange', () => {
      this.vt.reset();  // Set to a normal scale for if the mouse wheel was turned while full screen showing
    });
    this.video.ontimeupdate = () => {
      if (this.video.currentTime !== null && !isNaN(this.video.currentTime))
        this.currentTime = new Date(this.video.currentTime * 1000).toISOString().substring(11, 19);
      if (!this.isLive && this.video.duration !== null && !isNaN(this.video.duration))
        this.totalTime = new Date(this.video.duration * 1000).toISOString().substring(11, 19);
    };

    this.sizing = new VideoSizing(this.video);

    screen.orientation.addEventListener('change', this.orientationChangeHandler)  }

  ngOnDestroy(): void {
    this.mediaFeeder.stop();
    document.removeEventListener('click', this.clickHandler);

    // Calling stopAudioOut directly from ngOnDestroy leaves the backchannel in a state where no UDP output ids delivered from
    //  ffmpeg to the backchannel device. The problem does not occur when done like this
    let timerSubscription: Subscription = timer(20).subscribe(() => {
      this.audioBackchannel.stopAudioOut().then(r => {});
      timerSubscription.unsubscribe();
    });
    window.screen.orientation.onchange = null;
    this.sizing._destroy();
  }
}
