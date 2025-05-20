/// <reference lib="webworker" />

import {timer} from "rxjs";

let audioFeeder: AudioFeeder;
addEventListener('message', ({ data }) => {
  if(data.url) {
    if(typeof MediaSource !== 'undefined') {
      audioFeeder = new AudioFeeder(data.url);
      audioFeeder.setUpWSConnection();
    } else {
      let sub = timer(1000).subscribe(() => {
        sub.unsubscribe();
        postMessage({warning: true, warningMessage: "MediaSource is not supported in web workers on this browser. Audio is disabled"})
      });
    }
  }
  else if(data.close && audioFeeder)
    audioFeeder.close();
});

class AudioFeeder {

  private readonly url!:string;
  private timeout!:NodeJS.Timeout;
  private ws!: WebSocket;
  private noRestart: boolean = false;
  private mediaSource!: MediaSource;
  private sourceBuffer!: SourceBuffer;
  constructor(url: string) {
    this.url = url;
  }
  setUpWSConnection() {
    this.mediaSource = new MediaSource();
     // @ts-ignore
    postMessage({handle: this.mediaSource.handle}, [this.mediaSource.handle])
    this.mediaSource.addEventListener('sourceopen', (ev) => {
      this.sourceBuffer = this.mediaSource.addSourceBuffer('audio/aac');
      this.mediaSource.duration = 0.3;  // audio start duration for latency monitoring
    });
    let framesToMiss = 0;

    this.ws = new WebSocket(this.url);
    this.ws.binaryType = 'arraybuffer';

    this.ws.onerror = (ev) => {
      postMessage({warning: true, warningMessage: "An error occurred with the audio feeder websocket connection"})
      this.sourceBuffer.abort();
      this.mediaSource.endOfStream();
    }

    this.ws.onclose = (ev) => {
      this.sourceBuffer.abort();
      this.mediaSource.endOfStream();

      if(this.noRestart)
        postMessage({closed: true})
      console.warn("The audio feed websocket was closed: " + ev.reason);
     // clearTimeout(this.timeout);
    }

    this.timeout = setTimeout(() => {
      this.timedOut();
    }, 6000);

    this.ws.onmessage = async (event: MessageEvent) => {
      if (framesToMiss > 0)
        --framesToMiss;
      else {
        if(this.sourceBuffer && !this.sourceBuffer.updating) {
          this.sourceBuffer.appendBuffer(event.data);
        }
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
    if (this.sourceBuffer)
      this.sourceBuffer.abort();
    if (this.mediaSource)
      this.mediaSource.endOfStream();
   }

  close() {
    this.noRestart = true;
    if (this.ws)
      this.ws.close();
  }
}
