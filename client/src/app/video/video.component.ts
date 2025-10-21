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
import MediaFeeder from './MediaFeeder';
import {AudioBackchannel} from './AudioBackchannel';
import {VideoTransformations} from "./VideoTransformations";
import {VideoSizing} from "./VideoSizing";
import {SharedModule} from "../shared/shared.module";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {FormsModule} from "@angular/forms";
import AudioControlComponent from "./audio-control/audio-control.component";
import {animate, state, style, transition, trigger} from "@angular/animations";
import {NavComponent} from "../nav/nav.component";
import {AudioSettings} from "./AudioSettings";

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
  imports: [SharedModule, SharedAngularMaterialModule, FormsModule, AudioControlComponent, AudioControlComponent]
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
  visible: boolean = false;
  mediaFeeder!: MediaFeeder;
  multi: boolean = false;
  audioBackchannel!: AudioBackchannel
  vt!: VideoTransformations;
  currentTime: string = "";
  totalTime: string = "";
  sizing!: VideoSizing;
  showAudioControls: boolean = false;
  ctrlKeyDown = false;
  camKey: string = "";

  constructor(public utilsService: UtilsService) {
    this.mediaFeeder = new MediaFeeder();
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

  toggleMuteAudio() {
    if (this.mediaFeeder) {
      this.mediaFeeder.mute(!this.mediaFeeder.isMuted);
      const settings = new AudioSettings(this.mediaFeeder.volume, this.mediaFeeder.isMuted, this.mediaFeeder.getAudioLatencyControl());
      NavComponent.setCookie(this.camKey, JSON.stringify(settings), 600);
    }
  }

  mute(mute: boolean = true): void {
    if (this.mediaFeeder) {
      this.mediaFeeder.mute(mute);
      const settings = new AudioSettings(this.mediaFeeder.volume, mute, this.mediaFeeder.getAudioLatencyControl());
      NavComponent.setCookie(this.camKey, JSON.stringify(settings), 600);
    }
  }

  setVolume(volume: number) {
    if(this.mediaFeeder) {
      this.mediaFeeder.gain =volume;
      const settings = new AudioSettings(volume, this.mediaFeeder.isMuted, this.mediaFeeder.getAudioLatencyControl());
      NavComponent.setCookie(this.camKey, JSON.stringify(settings), 600);
    }
  }
  audioLatencyLimiting(audioLatencyLimiting: boolean) {
    if(this.mediaFeeder) {
     this.mediaFeeder.setAutoLatencyControl(audioLatencyLimiting);
     const settings = new AudioSettings(this.mediaFeeder.volume, this.mediaFeeder.isMuted, audioLatencyLimiting);
     NavComponent.setCookie(this.camKey, JSON.stringify(settings), 600);
    }
  }

  toggleShowAudioControls() {
    if(this.ctrlKeyDown) {
      this.toggleMuteAudio();
      this.showAudioControls = false;
    }
    else
      this.showAudioControls = !this.showAudioControls;
  }

  /**
   * getCamKey: Get key for level setting cookie name
   * @param isMulti True if multi-cam, otherwise false for single
   */
  getCamKey(isMulti: boolean): void {
    let camKey = '';
    const searchTerm = "?suuid=";
    const camIdx = this.stream.media_server_input_uri.indexOf(searchTerm);
    if (camIdx > -1) {
      const dashIdx = this.stream.media_server_input_uri.indexOf('-',camIdx);
      if (dashIdx > -1) {
        camKey = this.stream.media_server_input_uri.substring(camIdx+searchTerm.length, dashIdx);
      }
    }
    this.camKey = (isMulti ? 'multi-' : '') + camKey;
  }

  setInitialAudioSettings(isMulti: boolean, level: number, muted: boolean, audioLatencyLimiting: boolean) {
    this.getCamKey(isMulti);
    let audioSettings: AudioSettings = new AudioSettings(level, muted, audioLatencyLimiting);
    if(this.camKey !== "") {
      const strSettings = NavComponent.getCookie(this.camKey)
      if(strSettings !== "") {
        audioSettings = JSON.parse(strSettings);
      }
    }
    this.setVolume(audioSettings.level !== undefined ? audioSettings.level : 0.4);
    this.mute(audioSettings.mute !== undefined ? audioSettings.mute : false);
    this.audioLatencyLimiting(audioSettings.audioLatencyLimiting !== undefined ? audioSettings.audioLatencyLimiting : true);
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

  clickHandler =  (ev: Event) => {
    if(this.videoControlsEL) {
      const inVideoControlDialogue = ev.composedPath().includes(this.videoControlsEL.nativeElement);
      if (!inVideoControlDialogue)
        this.showAudioControls = false;
    }
  };

  keyHandler = (ev: KeyboardEvent): void => {
    this.ctrlKeyDown = ev.ctrlKey;
  }

  ngOnInit(): void {
    document.addEventListener('click', this.clickHandler);
    window.addEventListener("keydown", this.keyHandler);
    window.addEventListener("keyup", this.keyHandler);
  }

  ngAfterViewInit(): void {
    this.video = this.videoEl.nativeElement;
    this.mediaFeeder.init(this.isLive, this.video, this.reporting);
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

    screen.orientation.addEventListener('change', this.orientationChangeHandler)
  }

  ngOnDestroy(): void {
    this.mediaFeeder.stop();
    document.removeEventListener('click', this.clickHandler);
    window.removeEventListener("keydown", this.keyHandler)
    window.removeEventListener("keyup", this.keyHandler)
    // Calling stopAudioOut directly from ngOnDestroy leaves the backchannel in a state where no UDP output ids delivered from
    //  ffmpeg to the backchannel device. The problem does not occur when done like this
    let timerSubscription: Subscription = timer(20).subscribe(() => {
      this.audioBackchannel.stopAudioOut().then(r => {});
      timerSubscription.unsubscribe();
    });
    window.screen.orientation.onchange = null;
    this.sizing._destroy();
  }

  protected readonly MediaFeeder = MediaFeeder;
  protected readonly UtilsService = UtilsService;
}
