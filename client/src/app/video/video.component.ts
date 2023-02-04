import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraStream} from '../cameras/Camera';


declare let Hls: any;

@Component({
  selector: 'app-video',
  templateUrl: './video.component.html',
  styleUrls: ['./video.component.scss']
})
export class VideoComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('video') videoEl!: ElementRef<HTMLVideoElement>;

  @Input() isfmp4: boolean = false;
  camstream!: CameraStream;
  video!: HTMLVideoElement;
  hls: any = null;
  visible: boolean = false;
  recording: boolean = false;
  recordingUri: string = '';
  manifest: string = '';
  multi: boolean = false;

//  private isFullscreenNow: boolean = false;

  constructor() {
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
  private startVideo(): void {
    if (!this.isfmp4) {
      if (this.camstream !== undefined) {
        if (Hls.isSupported()) {
          this.hls = new Hls();
          this.hls.loadSource(this.recording ? this.recordingUri : this.camstream.stream.uri);
          this.hls.attachMedia(this.video);

          //hls.on(Hls.Events.MANIFEST_PARSED, this.video.play());
          // this.video.play();
        } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
          this.video.src = this.camstream.stream.uri;
        }
      }
    } else {
      this.ms = new MediaSource();
      this.ms.addEventListener('sourceopen', this.opened, false);

      // get reference to video
      //video.latencyHint = 0.075
      // set mediasource as source of video
      this.video.src = window.URL.createObjectURL(this.ms);
    }
  }

  toInt(arr:Uint8Array, index: number): number { // From bytes to big-endian 32-bit integer.  Input: Uint8Array, index
    let dv = new DataView(arr.buffer, 0);
    return dv.getInt32(index, false); // big endian
  }

  // toString(arr:number[], fr: number, to: number) { // From bytes to string.  Input: Uint8Array, start index, stop index.
  //   // https://developers.google.com/web/updates/2012/06/How-to-convert-ArrayBuffer-to-and-from-String
  //   return String.fromCharCode.apply(null, arr.slice(fr, to));
  // }

  verbose:boolean = false;
  buffering_sec: number = 0.8; // Default value
  buffering_sec_seek: number = this.buffering_sec * 0.9;
  // ..seek the stream if it's this much away or
  // from the last available timestamp
  buffering_sec_seek_distance: number = this.buffering_sec * 0.5;
  // .. jump to this distance from the last avail. timestamp
  started: boolean = false;

  mimeType: string = "video/mp4";

  stream_started = false; // is the source_buffer updateend callback active nor not

  // create media source instance
  ms!: MediaSource;

  // queue for incoming media packets
  queue:BufferSource[] = [];
  ws!: WebSocket; // websocket
  seeked = false; // have seeked manually once ..
  cc: number = 0;

  source_buffer!: SourceBuffer // source_buffer instance

  setlatencyLim(value: string) {
    this.buffering_sec = Number(value)
    // Update these accordingly
    this.buffering_sec_seek = this.buffering_sec * 0.9;
    this.buffering_sec_seek_distance = this.buffering_sec * 0.5;
  }
  Utf8ArrayToStr(array:Uint8Array):string {
    let out, i, len, c;
    let char2, char3;
    out = "";
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

  // consider these callbacks:
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
      let codecs  // https://wiki.whatwg.org/wiki/Video_type_parameters
      let decoded_arr = data.slice(1);
      if (window.TextDecoder) {
        codecs = new TextDecoder("utf-8").decode(decoded_arr);
      } else {
        codecs = this.Utf8ArrayToStr(decoded_arr);
      }
      if (this.verbose) {
        console.log('first packet with codec data: ' + codecs);
      }

      // if your stream has audio, remember to include it in these definitions.. otherwise your mse goes sour
      let codecPars = this.mimeType + ';codecs="' + codecs + '"';
      if (!(window.MediaSource && window.MediaSource.isTypeSupported(codecPars))) {
        console.log(codecPars + "Not supported");
      }
      this.ms.duration = this.buffering_sec;
      this.source_buffer = this.ms.addSourceBuffer(codecPars);

      // https://developer.mozilla.org/en-US/docs/Web/API/source_buffer/mode
      //     let myMode = source_buffer.mode;
      this.source_buffer.mode = 'sequence';
      // source_buffer.mode = 'segments';  // TODO: should we use this instead?

      this.source_buffer.addEventListener("updateend", this.loadPacket);
    } else if (this.started){
      // keep the latency to minimum
      let latest = this.video.duration;
      if ((this.video.duration >= this.buffering_sec) &&
        ((latest - this.video.currentTime) > this.buffering_sec_seek)) {
        console.log("seek from ", this.video.currentTime, " to ", latest);
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
        console.log("queue push:", this.queue.length);
      }
    }
  }
  loadPacket = () => { // called when source_buffer is ready for more
    if (!this.source_buffer.updating) { // really, really ready
      if (this.queue.length > 0) {

        let inp = this.queue.shift(); // pop from the beginning
        if (this.verbose) {
          console.log("this.queue pop:", this.queue.length);
        }

        let memview = new Uint8Array(inp as Uint8Array);

        if (this.verbose) {
          console.log(" ==> writing buffer with", memview[0], memview[1], memview[2], memview[3]);
        }

        this.source_buffer.appendBuffer(inp as BufferSource);
        this.cc += 1;
      } else { // the this.queue runs empty, so the next packet is fed directly
        this.stream_started = false;
      }
    } else { // so it was not?
    }
  }


  opened = () => { // MediaSource object is ready to go
    console.log("Called opened")
    // https://developer.mozilla.org/en-US/docs/Web/API/MediaSource/duration
    this.ws = new WebSocket(this.camstream.stream.uri);
    this.ws.binaryType = "arraybuffer";
    this.ws.onmessage = (event: MessageEvent) => {
      this.putPacket(event.data);
    };
  }

  stop(): void {
    if (!this.isfmp4 && this.hls !== null) {
      this.video.pause();
      this.hls.stopLoad();
      this.hls.detachMedia();
      this.hls.destroy();
      this.hls = null;
    }
    else if(this.isfmp4) {
      if (this.ws !== undefined)
        this.ws.close();
      this.started = false;
      this.video.pause();
    }
  }

  // private fullScreenListener:() =>void = ():void => {
  //   this.isFullscreenNow = document.fullscreenElement !== null
  //   if(this.isFullscreenNow)
  //     this.userIdle.stopWatching();
  //   else
  //     this.userIdle.startWatching();
  // };
  //
  ngOnInit(): void {
  }

  ngAfterViewInit(): void {

    this.video = this.videoEl.nativeElement;
    this.video.autoplay = true;
    this.video.muted = true;
    this.video.controls = true;

    // Stop the idle timeout if the video is being viewed full screen
    // this.video.addEventListener('fullscreenchange', this.fullScreenListener);
    // this.video.addEventListener('webkitfullscreenchange', this.fullScreenListener);

    // This prevents value changed after it was checked error
    //timer(10).subscribe(() => this.startVideo());
  }

  ngOnDestroy(): void {
    this.stop();
  }
}
