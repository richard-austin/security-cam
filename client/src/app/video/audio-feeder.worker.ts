/// <reference lib="webworker" />

import {timer} from "rxjs";

let audioFeeder: AudioWorker;
addEventListener('message', ({ data }) => {
  if(data.url) {
    // @ts-ignore
    if(typeof AudioDecoder !== 'undefined') {
      audioFeeder = new AudioWorker(data.url);
      audioFeeder.setUpWSConnection();
    } else {
      let sub = timer(1000).subscribe(() => {
        sub.unsubscribe();
        postMessage({warningMessage: "AudioDecoder is not supported on this browser"})
      });
    }
  }
  else if(data.close && audioFeeder)
    audioFeeder.close();
});

class AudioWorker {
// @ts-ignore
  private audioDecoder = new AudioDecoder({
    // @ts-ignore
    output: async (frame: AudioData) => {
      postMessage(frame, [frame]);
      frame.close();
    },
    error: (e: DOMException) => {
      console.warn("Audio decoder: " + e.message);
    },
  });

  private readonly config = {
    numberOfChannels: 1,
    sampleRate: 8000,  // Firefox hard codes to 48000
    codec: 'alaw',
  };

  private readonly url!:string;
  private timeout!:NodeJS.Timeout;
  private ws!: WebSocket;
  private noRestart: boolean = false;
  private started = false;
  constructor(url: string) {
    this.url = url;
  }
  setUpWSConnection() {
    this.ws = new WebSocket(this.url);
    this.ws.binaryType = 'arraybuffer';

    this.ws.onerror = (ev) => {
      postMessage({warningMessage: "An error occurred with the audio feeder websocket connection"})
    }

    this.ws.onclose = (ev) => {
      if(this.noRestart)
        postMessage({closed: true})
      console.warn("The audio feed websocket was closed: " + ev.reason);
     // clearTimeout(this.timeout);
    }

    this.timeout = setTimeout(() => {
      this.timedOut();
    }, 6000);

    this.ws.onmessage = async (event: MessageEvent) => {
      if (!this.started) {
        let array = new Uint8Array(event.data)
        if (array[0] === 9) {
          let decoded_arr = array.slice(1);
          let audioInfo = JSON.parse(this.Utf8ArrayToStr(decoded_arr));
          this.config.codec =audioInfo.codec_name == "aac" ? "mp4a.40.2" : "alaw";
          this.config.sampleRate = parseInt(audioInfo.sample_rate);
          this.audioDecoder.configure(this.config);
          console.log('first audio packet with codec data: ' + this.config.codec);
          this.started = true;
        } else
          console.error("No audio codec was found")
      } else {
             // @ts-ignore
             const eac = new EncodedAudioChunk({
               type: 'key',
               timestamp: 0,
               duration: 1,
               data: event.data,
             });
             await this.audioDecoder.decode(eac)
           }
      this.resetTimeout();
    };
  }

  resetTimeout = () => {
    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => {
      this.timedOut();
    }, 3000)
  }

  timedOut(){
    console.error("Audio feed from websocket has stopped...");
    if(this.ws)
      this.ws.close();
    if(this.audioDecoder)
      this.audioDecoder.close();
  }

  close() {
    this.noRestart = true;
    if (this.audioDecoder)
      this.audioDecoder.close();
    if (this.ws)
      this.ws.close();
  }
  Utf8ArrayToStr(array: Uint8Array): string {
    let out, i, len;
    out = '';
    len = array.length;
    i = 0;
    while (i < len) {
      out += String.fromCharCode(array[i]);
      ++i;
    }
    return out;
  }
}
