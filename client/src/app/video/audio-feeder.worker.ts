/// <reference lib="webworker" />

let audioFeeder: AudioFeeder;
addEventListener('message', ({ data }) => {
  if(data.url) {
    audioFeeder = new AudioFeeder(data.url);
    audioFeeder.setUpWSConnection();
  }
  else if(data.close && audioFeeder)
    audioFeeder.close();
});

class AudioFeeder {
// @ts-ignore
  private audioDecoder = new AudioDecoder({
    // @ts-ignore
    output: async (frame: AudioData) => {
      postMessage(frame);
      frame.close();
    },
    error: (e: DOMException) => {
      console.warn("Audio decoder: " + e.message);
    },
  });

  private readonly config = {
    numberOfChannels: 1,
    sampleRate: 16000,  // Firefox hard codes to 48000
    codec: 'mp4a.40.2',
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
    let framesToMiss = 12;

    this.ws = new WebSocket(this.url);
    this.ws.binaryType = 'arraybuffer';

    this.ws.onerror = (ev) => {
      console.error("An error occurred with the audio feeder websocket connection")
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
      // @ts-ignore
      const eac = new EncodedAudioChunk({
        type: 'key',
        timestamp: 0,
        duration: 1,
        data: event.data,
      });
      if (framesToMiss > 0)
        --framesToMiss;
      else
        await this.audioDecoder.decode(eac)
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
    this.ws.close()
  }
}
