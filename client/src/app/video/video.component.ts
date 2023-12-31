import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Camera, Stream} from '../cameras/Camera';
import {Client} from "@stomp/stompjs";
import {UtilsService} from '../shared/utils.service';
import {ReportingComponent} from "../reporting/reporting.component";
import {interval, Subscription, timer} from "rxjs";
import { MediaFeeder } from './MediaFeeder';

declare let Hls: any;

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.scss']
})
export class VideoComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('video') videoEl!: ElementRef<HTMLVideoElement>;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @Input() isfmp4: boolean = false;
  cam!: Camera;
  stream!: Stream;
  video!: HTMLVideoElement;

  visible: boolean = false;
 // manifest: string = '';
  // @ts-ignore
  recorder: MediaRecorder;
  mediaDevices!: MediaDeviceInfo[];
  audioToggle: boolean = false;
  selectedDeviceId!: string;
  selectedAudioInput!: MediaDeviceInfo;
//  private isFullscreenNow: boolean = false;
  private client!: Client;
  private timeForStartAudioOutResponse: number = 0;
  feeder!: MediaFeeder;

  constructor(public utilsService: UtilsService) {
    this.feeder = new MediaFeeder()
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param cam
   * @param stream
   * @param manifest
   */
  setSource(cam: Camera, stream: Stream, manifest: string = ''): void {
    this.stream = stream;
    this.feeder.setSource(cam, stream, manifest)
    if(cam.backchannelAudioSupported) {
      // Call getUserMedia to make the browser ask the user for permission to access the microphones so that
      //  enumerateDevices can get the microphone list.
      navigator.mediaDevices.getUserMedia({
        audio: true,
        video: false
      }).then(() => {
        navigator.mediaDevices.enumerateDevices().then((dev) => {
          this.selectedAudioInput = dev[0];
          this.selectedDeviceId = this.selectedAudioInput.deviceId;
          this.mediaDevices = dev;
        }).catch((error) => {
          this.reporting.errorMessage = error;
        });
      })
    }
  }

  multi: boolean = false;
  stop(): void {
    this.feeder.stop();
  }

  toggleAudioOut() {
    if(!this.utilsService.isGuestAccount && (!this.utilsService.speakActive || this.audioToggle)) {
      this.audioToggle = !this.audioToggle;
      if (this.audioToggle) {
        this.timeForStartAudioOutResponse = 0;
        // Time the response and add this to the audio off delay time, this is a bodge to mitigate cutting the audio
        //  off before outgoing voice message was complete.
        let intervalSubscription: Subscription = interval(1).subscribe(() => ++this.timeForStartAudioOutResponse)
        this.utilsService.startAudioOut(this.stream).subscribe(() => {
          intervalSubscription.unsubscribe();
        }, reason => {
          this.reporting.errorMessage = reason
          this.stopAudioOut();
        });
        this.startAudioOutput();
      }
      else {
        this.beginStopAudioOut();
      }
    }
  }

  setAudioInput() {
    // @ts-ignore
    this.selectedAudioInput =
      this.mediaDevices.find((dev) => {
        return dev.deviceId === this.selectedDeviceId
      })
  }

  startAudioOutput(): boolean {
    let retVal = true;
    this.video.muted = true;
    let serverUrl: string = (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/audio';

    this.client  = new Client({
      brokerURL: serverUrl,
      onConnect: () => {},
      reconnectDelay: 1000,
      debug: () => {}
    });

    if (navigator.mediaDevices) {
      navigator.mediaDevices.getUserMedia({
        audio: (this.selectedAudioInput == null ? true : {deviceId: this.selectedAudioInput.deviceId}),
        video: false
      }).then((stream) => {
        const mimeType = 'video/webm;codecs=vp8,opus';
        // @ts-ignore
        if (!MediaRecorder.isTypeSupported(mimeType)) {
          alert('vp8/opus mime type is not supported');
          return;
        }
        const options = {
          audioBitsPerSecond: 48000,
          mimeType,
        }

        // @ts-ignore
        this.recorder = new MediaRecorder(stream, options);
        // fires every one second and passes a BlobEvent
        this.recorder.ondataavailable = (event: any) => {
          // get the Blob from the event
          const blob: Blob = event.data;
          blob.arrayBuffer().then((buff) => {
            let data = new Uint8Array(buff);

            if(data.length > 0) {
              // and send that blob to the server...
              this.client.publish({
                destination: '/app/audio',
                binaryBody: data,
                headers: {"content-type": "application/octet-stream"}
              });
            }
          });
        };

        this.recorder.onstop = () => {
          this.recorder.ondataavailable = undefined;
          this.recorder.onstop = undefined;
        }

        this.client.onConnect = () => this.recorder.start(100);
        this.client.activate();

        // This stops the audio out after 5 minutes
        let stopAudioAfterLongTimeSubscription = timer(300000).subscribe(() => {
          this.stopAudioOut();
          stopAudioAfterLongTimeSubscription.unsubscribe();
        });
      }).catch((error) => {
        this.stopAudioOut();
        retVal = false;
        this.reporting.errorMessage = error;
        console.log(error);
      });
    }
    return retVal;
  }

  beginStopAudioOut() {
    // 1.6 second plus timeForStartAudioOutResponse delay on stopping to allow for the latency in the audio and prevent
    //  the end of the speech getting get cut off
    let timerSubscription = timer(1600+this.timeForStartAudioOutResponse).subscribe(() => {
      this.stopAudioOut();
      timerSubscription.unsubscribe();
    });
  }

  stopAudioOut() : void {
    this.video.muted = false;
    this.recorder?.stop();
    this.utilsService.stopAudioOut().subscribe(() => {
      this.client?.deactivate({force: false}).then(() => {});
    }, reason => {
      this.reporting.errorMessage = reason;
    });
    this.audioToggle = false;
  }

  audioButtonTooltip() : string {
    let speakActive = this.utilsService.speakActive;
    let retVal: string = "";
    if(this.utilsService.isGuestAccount)
      retVal = "Not available to guest"
    else if(speakActive)
      retVal = this.audioToggle ? "Stop audio to camera" : "Audio to camera is in use in another session. Cannot start audio to camera at this time";
    else if (!speakActive) {
      retVal = "Start audio to camera"
    }
    return retVal;
  }

  audioInputSelectorTooltip() {
    let retVal = "Set audio input device";
    if(this.utilsService.speakActive)
      retVal = "Audio to camera is in use, cannot select audio input at this time";
    return retVal;
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.feeder.init(this.isfmp4, this.videoEl.nativeElement, this);
    this.video = this.videoEl.nativeElement;
    // @ts-ignore
    this.selectedAudioInput = null;
  }

  ngOnDestroy(): void {
    this.feeder.stop();
    if (this.audioToggle) {
      // Calling stopAudioOut directly from ngOnDestroy leaves the backchannel in a state where no UDP output ids delivered from
      //  ffmpeg to the backchannel device. The problem does not occur when done like this
      let timerSubscription: Subscription =  timer(20).subscribe( () => {
        this.stopAudioOut();
        timerSubscription.unsubscribe();
      });
     }
  }
}
