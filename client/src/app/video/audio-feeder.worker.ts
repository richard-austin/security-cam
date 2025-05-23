/// <reference lib="webworker" />

import {timer} from "rxjs";

let audioFeeder: AudioFeeder;
addEventListener('message', ({ data }) => {
  if(data.url) {
    // @ts-ignore
    if(typeof AudioDecoder !== 'undefined') {
      audioFeeder = new AudioFeeder(data.url);
      audioFeeder.setUpWSConnection();
    } else {
      let sub = timer(1000).subscribe(() => {
        sub.unsubscribe();
        postMessage({media: false, warningMessage: "AudioDecoder is not supported on this browser"})
      });
    }
  }
  else if(data.close && audioFeeder)
    audioFeeder.close();
});

class AudioFeeder {
// @ts-ignore
  private audioDecoder = new AudioDecoder({
    // @ts-ignore
    output: async (frame: AudioData) => {
      postMessage({media: true, packet: frame});
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
  constructor(url: string) {
    this.url = url;
  }
  setUpWSConnection() {
    this.audioDecoder.configure(this.config);
    let framesToMiss = 0;

    this.ws = new WebSocket(this.url);
    this.ws.binaryType = 'arraybuffer';

    this.ws.onerror = (ev) => {
      postMessage({media: false, warningMessage: "An error occurred with the audio feeder websocket connection"})
    }

    this.ws.onclose = (ev) => {
      if(this.noRestart)
        postMessage({media: false, closed: true})
      console.warn("The audio feed websocket was closed: " + ev.reason);
     // clearTimeout(this.timeout);
    }

    this.timeout = setTimeout(() => {
      this.timedOut();
    }, 6000);

    this.ws.onmessage = async (event: MessageEvent) => {
      // @ts-ignore
      const eac = new EncodedAudioChunk({
        type: 'key',
        timestamp: 0,
        duration: 1,
        data: event.data,
      });
      if (framesToMiss > 0)
        --framesToMiss;
      else {
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
}
