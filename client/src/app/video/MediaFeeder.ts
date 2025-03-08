import Hls from "hls.js";
import {Camera, Stream} from "../cameras/Camera";
import {interval, Subscription, timer} from "rxjs";

export class MediaFeeder {
  isfmp4!: boolean;
  cam!: Camera;
  stream!: Stream;
  media!: HTMLMediaElement;
  hls: any = null;
  recording: boolean = false;
  manifest: string = '';
  recordingUri: string = '';
  // queue for incoming media packets
  queue: BufferSource[] = [];
  ws!: WebSocket; // websocket
  stream_started = false; // is the source_buffer updateend callback active or not
  started: boolean = false;
  startLatencyChasing: boolean = false;
  mimeType: string = 'video/mp4';

  // create media source instance
  ms!: MediaSource;
  cc: number = 0;
  source_buffer!: SourceBuffer; // source_buffer instance
  verbose: boolean = false;
  buffering_sec!: number
  buffering_sec_seek!: number;
  // Seek the stream if it's this much away or
  // from the last available timestamp
  buffering_sec_seek_distance!: number;
  latency_chasing: boolean = false;

  isAudio: boolean;
  // .. jump to this distance from the last avail. timestamp

  constructor(buffering_sec: number, isAudio: boolean = false) {
    this.setlatencyLim(buffering_sec);
    this.isAudio = isAudio
  }

  init(isfmp4: boolean, media: HTMLMediaElement) {
    this.isfmp4 = isfmp4;
    this.media = media;
    this.media.autoplay = true;
    this.media.muted = false;
    this.media.controls = !isfmp4;
  }

  /**
   * setSource: Set up to play the given manifest file and display the camera details
   * @param cam
   * @param stream
   * @param manifest
   */
  setSource(cam: Camera, stream: Stream, manifest: string = ''): void {
    this.stop();
    this.cam = cam;
    this.stream = stream;
    this.recording = manifest !== '';
    this.recordingUri = stream.recording.uri;

    if (this.recordingUri[this.recordingUri.length - 1] !== '/') {
      this.recordingUri += '/';
    }

    this.recordingUri += manifest;
    this.manifest = manifest;   // Save the manifest file name so it can be returned by getManifest
    this.startVideo();
  }

  /**
   * startVideo: Start the video (assumes appropriate uri and camera is set up).
   * @private
   */
  public startVideo(): void {
    if (!this.isfmp4) {
      if (this.cam !== undefined) {
        if (Hls.isSupported()) {
          this.hls = new Hls();
          this.hls.loadSource(this.recording ? this.recordingUri : this.stream.uri);
          this.hls.attachMedia(this.media);
        } else if (this.media.canPlayType('application/vnd.apple.mpegurl')) {
          this.media.src = this.stream.uri;
        }
      }
    } else {
      this.ms = new MediaSource();
      this.ms.addEventListener('sourceopen', this.opened, false);

      // set MediaSource as source of video
      this.media.src = window.URL.createObjectURL(this.ms);
      this.media.onplay = () => {
        console.log("Playing");
        this.startLatencyChasing = true;
      };
    }
  }

  streamTestInterval!: Subscription;

  opened = () => { // MediaSource object is ready to go
    console.log('Called opened');
    // https://developer.mozilla.org/en-US/docs/Web/API/MediaSource/duration
    let getUrl = window.location;
    let baseUrl = getUrl.protocol + '//' + getUrl.host + '/' + getUrl.pathname.split('/')[1];

    let url = this.stream.uri.startsWith('http:') || this.stream.uri.startsWith('https:') || this.stream.uri.startsWith('ws:')
      ?  // Absolute url
      this.stream.uri
        .replace('https', 'wss') // Change https to wss
        .replace('http', 'ws')  // or change http to ws
      :  // Relative uri
      baseUrl.substring(0, baseUrl.length - 1) // Remove trailing /
        .replace('https', 'wss') // Change https to wss
        .replace('http', 'ws')  // or change http to ws
      + this.stream.uri;
    url += this.isAudio ? 'a' : '';

    let counter = 0;
    this.streamTestInterval = interval(1000).subscribe(() => {
      if (++counter >= 4) {  // If counter gets to 4, the stream messages have stopped, so restart the video
        console.log("Stream for " + this.cam.name + " has stalled, attempting restart in 1 second");
        this.stop();  // Close the existing video set up
        let timerSubscription = timer(1000).subscribe(() => {
          timerSubscription.unsubscribe();
          this.startVideo();  // Start it again after 1-second delay
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
      this.media.pause();
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
      this.media.pause();
    }
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
      this.startLatencyChasing = false;
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
      // this.source_buffer.mode = 'segments';  // TODO: should we use this instead?

      this.source_buffer.addEventListener('updateend', this.loadPacket);
    } else if (this.started) {
      // keep the latency to minimum
      let latest = this.media.duration;

      if (this.startLatencyChasing && (this.media.duration >= this.buffering_sec) &&
        ((latest - this.media.currentTime) > this.buffering_sec_seek)) {
        console.log('seek from ', this.media.currentTime, ' to ', latest);
        // Flag to show latency chasing is occurring
        this.latency_chasing = true;
        let timerSubscription = timer(500).subscribe(() => {
          this.latency_chasing = false;
          timerSubscription.unsubscribe();
        });
        let df = (this.media.duration - this.media.currentTime); // this much away from the last available frame
        if ((df > this.buffering_sec_seek)) {
          this.media.currentTime = this.media.duration - this.buffering_sec_seek_distance;
        }
      }

      data = arr;
      if (!this.stream_started && this.media.error == null) {
        this.source_buffer.appendBuffer(data);
        this.stream_started = true;
        this.cc += 1;
        return;
      } else if (this.media.error != null)
        console.log(this.media.error.message)

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

  setlatencyLim(buffering_sec: number | object) {
    this.buffering_sec = Number(buffering_sec);
    // Update these accordingly
    this.buffering_sec_seek = this.buffering_sec * 0.9;
    this.buffering_sec_seek_distance = this.buffering_sec * 0.5;
  }

  get isLatencyChasing(): boolean {
    return this.latency_chasing;
  }
  mute(muted: boolean = true) {
    if(this.media !== null && this.media !== undefined)
      this.media.muted = muted;
  }

  get isMuted() {
    return this.media !== null && this.media !== undefined && this.media.muted;
  }

  get hasCam(): boolean {
    return this.cam !== null && this.cam !== undefined;
  }
  get hasStream(): boolean {
    return this.stream !== null && this.stream !== undefined;
  }
  get backchannelAudioSupported(): boolean {
    return this.hasCam && this.cam.backchannelAudioSupported
  }
  get camName(): string {
    return this.hasCam ? this.cam.name : 'NO CAMERA!!';
  }

  get streamDescr() : string {
    return this.hasStream ? this.stream.descr : "NO STREAM!!"
  }

  get camera() : Camera {
    return this.cam;
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
}
