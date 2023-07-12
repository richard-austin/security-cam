import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraStream} from '../cameras/Camera';
import {MatSelect} from '@angular/material/select';
import {Client} from "@stomp/stompjs";
import {UtilsService} from '../shared/utils.service';
import {ReportingComponent} from "../reporting/reporting.component";
import {interval, Subscription, timer} from "rxjs";

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
  camstream!: CameraStream;
  video!: HTMLVideoElement;

  hls: any = null;
  visible: boolean = false;
  recording: boolean = false;
  recordingUri: string = '';
  manifest: string = '';
  // @ts-ignore
  recorder: MediaRecorder;
  mediaDevices!: MediaDeviceInfo[];
  audioToggle: boolean = false;
  selectedDeviceId!: string;
  selectedAudioInput!: MediaDeviceInfo;
  stopAudioAfterLongTimeSubscription!: Subscription

//  private isFullscreenNow: boolean = false;
  private client!: Client;
  private timeForStartAudioOutResponse: number = 0;


  constructor(public utilsService: UtilsService) {
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param camStream: The camera
   * @param manifest: The manifest file
   */
  setSource(camStream: CameraStream, manifest: string = ''): void {
    this.stop();
    this.camstream = camStream;
    this.recording = manifest !== '';
    this.recordingUri = camStream.stream.recording.uri;

    if (this.recordingUri[this.recordingUri.length - 1] !== '/') {
      this.recordingUri += '/';
    }

    if(camStream.camera.backchannelAudioSupported) {
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

    this.recordingUri += manifest;
    this.manifest = manifest;   // Save the manifest file name so it can be returned by getManifest
    this.startVideo();
  }

  /**
   * Get the currently selected recording manifest file
   */
  getManifest(): string {
    return this.manifest;
  }

  /**
   * startVideo: Start the video (assumes appropriate uri and camera is set up).
   * @private
   */
  private startVideo(keepAudioStream: boolean = false): void {
    if (!this.isfmp4) {
      if (this.camstream !== undefined) {
        if (Hls.isSupported()) {
          this.hls = new Hls();
          this.hls.loadSource(this.recording ? this.recordingUri : this.camstream.stream.uri);
          this.hls.attachMedia(this.video);
        } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
          this.video.src = this.camstream.stream.uri;
        }
      }
    } else {
      // If an audio output session is running, stop it if we switch video views.
      if(!keepAudioStream && this.audioToggle)
        this.stopAudioOut();

      this.ms = new MediaSource();
      this.ms.addEventListener('sourceopen', this.opened, false);

      // set MediaSource as source of video
      this.video.src = window.URL.createObjectURL(this.ms);
    }
  }

  verbose: boolean = false;
  multi: boolean = false;
  buffering_sec: number = 1.2; // Default value, changeable from live video pages.
  buffering_sec_seek: number = this.buffering_sec * 0.9;
  // ..seek the stream if it's this much away or
  // from the last available timestamp
  buffering_sec_seek_distance: number = this.buffering_sec * 0.5;
  // .. jump to this distance from the last avail. timestamp
  started: boolean = false;

  mimeType: string = 'video/mp4';

  stream_started = false; // is the source_buffer updateend callback active nor not

  // create media source instance
  ms!: MediaSource;

  // queue for incoming media packets
  queue: BufferSource[] = [];
  ws!: WebSocket; // websocket
  seeked = false; // have seeked manually once ..
  cc: number = 0;

  source_buffer!: SourceBuffer; // source_buffer instance

  setlatencyLim() {
    this.buffering_sec = Number(this.buffering_sec);
    // Update these accordingly
    this.buffering_sec_seek = this.buffering_sec * 0.9;
    this.buffering_sec_seek_distance = this.buffering_sec * 0.5;
  }

  Utf8ArrayToStr(array: Uint8Array): string {
    let out, i, len, c;
    let char2, char3;
    out = '';
    len = array.length;
    i = 0;
    while (i < len) {
      c = array[i++];
      switch (c >> 4) {
        case 7:
          out += String.fromCharCode(c);
          break;
        case 13:
          char2 = array[i++];
          out += String.fromCharCode(((c & 0x1F) << 6) | (char2 & 0x3F));
          break;
        case 14:
          char2 = array[i++];
          char3 = array[i++];
          out += String.fromCharCode(((c & 0x0F) << 12) |
            ((char2 & 0x3F) << 6) |
            ((char3 & 0x3F) << 0));
          break;
      }
    }
    return out;
  }


  // Callbacks:
  // - putPacket : called when websocket receives data
  // - loadPacket : called when source_buffer is ready for more data
  // Both operate on a common fifo
  putPacket(arr: Uint8Array) {
    // receives ArrayBuffer.  Called when websocket gets more data
    // first packet ever to arrive: write directly to source_buffer
    // source_buffer ready to accept: write directly to source_buffer
    // otherwise insert it to queue

    let data = new Uint8Array(arr);
    if (data[0] === 9 && !this.started) {
      this.started = true;
      let codecs;  // https://wiki.whatwg.org/wiki/Video_type_parameters
      let decoded_arr = data.slice(1);
      if (window.TextDecoder) {
        codecs = new TextDecoder('utf-8').decode(decoded_arr);
      } else {
        codecs = this.Utf8ArrayToStr(decoded_arr);
      }
      if (this.verbose) {
        console.log('first packet with codec data: ' + codecs);
      }

      // if your stream has audio, remember to include it in these definitions.. otherwise your mse goes sour
      let codecPars = this.mimeType + ';codecs="' + codecs + '"';
      if (!(window.MediaSource && window.MediaSource.isTypeSupported(codecPars))) {
        console.log(codecPars + 'Not supported');
      }
      this.ms.duration = this.buffering_sec;
      this.source_buffer = this.ms.addSourceBuffer(codecPars);

      // https://developer.mozilla.org/en-US/docs/Web/API/source_buffer/mode
      //     let myMode = source_buffer.mode;
      this.source_buffer.mode = 'sequence';
      // source_buffer.mode = 'segments';  // TODO: should we use this instead?

      this.source_buffer.addEventListener('updateend', this.loadPacket);
    } else if (this.started) {
      // keep the latency to minimum
      let latest = this.video.duration;
      if ((this.video.duration >= this.buffering_sec) &&
        ((latest - this.video.currentTime) > this.buffering_sec_seek)) {
        console.log('seek from ', this.video.currentTime, ' to ', latest);
        let df = (this.video.duration - this.video.currentTime); // this much away from the last available frame
        if ((df > this.buffering_sec_seek)) {
          this.video.currentTime = this.video.duration - this.buffering_sec_seek_distance;
        }
      }

      data = arr;
      if (!this.stream_started) {
        this.source_buffer.appendBuffer(data);
        this.stream_started = true;
        this.cc += 1;
        return;
      }

      this.queue.push(data); // add to the end
      if (this.verbose) {
        console.log('queue push:', this.queue.length);
      }
    }
  }

  loadPacket = () => { // called when source_buffer is ready for more
    if (!this.source_buffer.updating) { // really, really ready
      if (this.queue.length > 0) {

        let inp = this.queue.shift(); // pop from the beginning
        if (this.verbose) {
          console.log('this.queue pop:', this.queue.length);
        }

        let memview = new Uint8Array(inp as Uint8Array);

        if (this.verbose) {
          console.log(' ==> writing buffer with', memview[0], memview[1], memview[2], memview[3]);
        }

        this.source_buffer.appendBuffer(inp as BufferSource);
        this.cc += 1;
      } else { // the this.queue runs empty, so the next packet is fed directly
        this.stream_started = false;
      }
    } else { // so it was not?
    }
  };

  streamTestInterval!: Subscription;

  opened = () => { // MediaSource object is ready to go
    console.log('Called opened');
    // https://developer.mozilla.org/en-US/docs/Web/API/MediaSource/duration
    let getUrl = window.location;
    let baseUrl = getUrl.protocol + '//' + getUrl.host + '/' + getUrl.pathname.split('/')[1];

    let url = this.camstream.stream.uri.startsWith('http:') || this.camstream.stream.uri.startsWith('https:') || this.camstream.stream.uri.startsWith('ws:')
      ?  // Absolute url
      this.camstream.stream.uri
        .replace('https', 'wss') // Change https to wss
        .replace('http', 'ws')  // or change http to ws
      :  // Relative uri
      baseUrl.substring(0, baseUrl.length - 1) // Remove trailing /
        .replace('https', 'wss') // Change https to wss
        .replace('http', 'ws')  // or change http to ws
      + this.camstream.stream.uri;
    let counter = 0;
    this.streamTestInterval = interval(1000).subscribe(() => {
      if(++counter >= 4) {  // If counter gets to 4, the stream messages have stopped, so restart the video
        console.log("Stream for "+this.camstream.camera.name+" has stalled, attempting restart in 1 second");
        this.stop();  // Close the existing video set up
        let timerSubscription = timer(1000).subscribe(() => {
          timerSubscription.unsubscribe();
          this.startVideo(true);  // Start it again after 1-second delay
        });
      }
    });
    this.ws = new WebSocket(url);
    this.ws.binaryType = 'arraybuffer';
    this.ws.onmessage = (event: MessageEvent) => {
      this.putPacket(event.data);
      counter = 0;
    };
  };

  stop(): void {
    this.streamTestInterval?.unsubscribe();  // Stop the stream test interval, or it will be restarted after
                                            // we exit the video component
    if (!this.isfmp4 && this.hls !== null) {
      this.video.pause();
      this.hls.stopLoad();
      this.hls.detachMedia();
      this.hls.destroy();
      this.hls = null;
    } else if (this.isfmp4) {
      if (this.ws !== undefined) {
        this.ws.close();
        this.ws.onmessage = null;
      }
      this.started = false;
      this.video.pause();
    }
  }

  toggleAudioOut() {
    if(!this.utilsService.speakActive || this.audioToggle) {
      this.audioToggle = !this.audioToggle;
      if (this.audioToggle) {
        this.timeForStartAudioOutResponse = 0;
        // Time the response and add this to the audio off delay time, this is a bodge to mitigate cutting the audio
        //  off before outgoing voice message was complete.
        let intervalSubscription: Subscription = interval(1).subscribe(() => ++this.timeForStartAudioOutResponse)
        this.utilsService.startAudioOut(this.camstream.stream).subscribe(() => {
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
        // @ts-ignore
        this.recorder = new MediaRecorder(stream);
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

        this.client.onConnect = () => this.recorder.start(100);
        this.client.activate();

        // This stops the audio out after 5 minutes
        this.stopAudioAfterLongTimeSubscription = timer(300000).subscribe(() => {
          this.stopAudioOut();
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
    timer(1600+this.timeForStartAudioOutResponse).subscribe(() => {
      this.stopAudioOut();
    });
  }

  private stopAudioOut() : void {
    this.video.muted = false;
    this.stopAudioAfterLongTimeSubscription?.unsubscribe();
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
    if(speakActive)
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
    this.video = this.videoEl.nativeElement;
    this.video.autoplay = true;
    this.video.muted = true;
    this.video.controls = true;
    // @ts-ignore
    this.selectedAudioInput = null;
  }

  ngOnDestroy(): void {
    this.stop();
    if (this.audioToggle) {
      this.stopAudioOut();
     }
  }
}
