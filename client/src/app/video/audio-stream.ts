//
// Notes:-
// This is derived from the MediaStreamTrackGenerator polyfill used for browsers not supporting MediaStreamTrackGenerator.
// The polyfill as originally obtained doesn't work properly with audio as the AudioContext seems to default to sample rate
// 48000. This is fixed here by passing the correct sample rate to the AudioContext constructor. There is some audio latency drift
// when the media stream track is passed to MediaStream for output to a media element. This is fixed here by monitoring the arrays (queue) size
// using the BufferStats class and shifting out excess packets from the queue. As this is used on Chrome browsers now,
// the audioData format in the write function (from the decoder) must be checked as it is Int16 with G711 audio, and
// Float32 with AAC, and the correct array type must be set up. Additionally, a very large amount of attenuation is
// required (by setting the gain value) for Int16.

interface iBufferStats {
  update(val: number): void;
  average() : number;
  standardDeviation(): number;
  reset(): void;
  bufferTooLarge(bufferSize: number): boolean;
  getWorkingBufferSize(): number;
}

class MyUnderlyingSink implements UnderlyingSink {
  ac: AudioContext;
  emptyArrayMap!: Map<number, Int16Array<ArrayBuffer> | Float32Array<ArrayBuffer> | undefined>;
  node!: AudioWorkletNode;
  gainNode: GainNode;
  gainFactor: number = 0;
  gain: number = 1;
  muted: boolean = false;

  constructor(ac: AudioContext) {
    this.ac = ac;
    this.gainNode = ac.createGain();
    this.gainNode.connect(ac.destination);
  }

  async start() {
    this.emptyArrayMap = new Map();

    function worklet() {
      /* @ts-ignore */
      registerProcessor("audio-feeder", class Processor extends AudioWorkletProcessor {
        static get parameterDescriptors() {
          return [
            {
              name: "autoLatencyControl",
              defaultValue: true,
              automationRate: "a-rate",
            },
          ];
        }

        arrays: [number][] = [];
        array: number[] = [];
        arrayOffset = 0;
        emptyArray!: number[];
        autoLatencyControl: boolean;
        running = true;
        bs: iBufferStats;

        constructor() {
          super();
          /* BufferStats class must be declared here as it is instantiated in the AudioContext */
          this.bs = new class BufferStats implements iBufferStats {
            private arraySize = 5;
            statsArray = new Array(this.arraySize);
            arrayIndex = 0;
            fullArray = false;
            updated = false;
            lastAverage = 0;
            sdUpdated = false;
            lastSd = 0;
            /* @ts-ignore */
            startTime = currentTime;
            measurementWindowSecs = 20;
            measuring = true;
            maxBufferSize = 20;
            workingBufferSize = 5;
            maxBufferSizeHardLimit = 15;
            workingBufferSizeHardLimit = 8;

            update(val: number) {
              if (this.measuring) {
                this.updated = this.sdUpdated = true;
                this.statsArray[this.arrayIndex++] = val;
                if (!this.fullArray && this.arrayIndex === this.arraySize)
                  this.fullArray = true;
                if (this.arrayIndex >= this.arraySize)
                  this.arrayIndex = 0;
                /* @ts-ignore */
                const timeNow = currentTime;
                if (timeNow - this.startTime > this.measurementWindowSecs) {
                  if (this.fullArray) {
                    this.maxBufferSize = Math.round(this.average() + 3 + this.standardDeviation());
                    this.workingBufferSize = Math.round(this.average() - this.standardDeviation());
                    if (this.workingBufferSize < 1)
                      this.workingBufferSize = 1;
                    else if (this.workingBufferSize > this.workingBufferSizeHardLimit)
                      this.workingBufferSize = this.workingBufferSizeHardLimit;

                    /* Clamp maxBufferSize to maxBufferSizeHardLimit if greater than that value */
                    this.maxBufferSize =
                      this.maxBufferSize > this.maxBufferSizeHardLimit ?
                        this.maxBufferSizeHardLimit :
                        this.maxBufferSize;

                    this.startTime = timeNow;
                    this.measuring = false;
                    console.debug("maxBufferSize set to ", this.maxBufferSize + " workingBufferSize set to " + this.workingBufferSize);
                  } else {
                    console.error("Moving average array not full, cannot calculate bufferSize");
                  }
                }
              }
            }

            average() : number {
              if (this.updated) {
                let sum = 0;
                for (let i = 0; i < this.arraySize; i++) {
                  sum += this.statsArray[i];
                }
                this.lastAverage = sum / this.arraySize;
                this.updated = false;
              }
              return this.lastAverage;
            }

            standardDeviation() : number {
              if (this.sdUpdated) {
                const average = this.average();
                let variance = 0;
                for (let i = 0; i < this.arraySize; i++) {
                  variance += (this.statsArray[i] - average) ** 2;
                }
                variance /= this.arraySize;
                this.lastSd = Math.sqrt(variance);
                this.sdUpdated = false;
              }
              return this.lastSd;
            }

            reset(): void {
              this.arrayIndex = 0;
              this.fullArray = false;
              this.updated = false;
              this.lastAverage = 0;
              this.sdUpdated = false;
              this.lastSd = 0;
              /* @ts-ignore */
              this.startTime = currentTime;
              this.measuring = true;
            }

            bufferTooLarge(bufferSize: number): boolean {
              const bufferTooLarge = !this.measuring && bufferSize > this.maxBufferSize;
              if (bufferTooLarge)
                this.reset();
              return bufferTooLarge;
            }

            getWorkingBufferSize(): number {
              return this.workingBufferSize;
            }
          }
          ;
          this.arrays = [];
          this.array = [];
          this.arrayOffset = 0;
          this.autoLatencyControl = true;
          this.running = true;
          let lastAutoLatencyControl = this.autoLatencyControl;
          /* @ts-ignore */
          this.port.onmessage = ({data}) => {
            if (data?.type === 'shutdown') {
              this.running = false;
              console.log("running = false");
            } else {
              /*      console.log("Pushing, data length = "+data?.length); */
              this.arrays.push(data);
              if (this.autoLatencyControl && !lastAutoLatencyControl) {
                /* @ts-ignore */
                this.port.postMessage("Auto latency control enabled");
                this.bs.reset();
                this.arrays = this.arrays.slice(this.arrays.length - 1);
              }
              lastAutoLatencyControl = this.autoLatencyControl;
              if (this.autoLatencyControl) {
                this.bs.update(this.arrays.length);
                /* Prevent audio latency build up due to delayed packets etc. */
                if (this.bs.bufferTooLarge(this.arrays.length)) {
                  /* @ts-ignore */
                  this.port.postMessage("Reducing audio packets queue from " + this.arrays.length + " to " + this.bs.getWorkingBufferSize());
                  while (this.arrays.length > this.bs.getWorkingBufferSize())
                    this.arrays.shift();
                }
              }
            }
          }
          this.emptyArray = [];
        }

        /* Audio worklet processor function */
        process(inputs: number[][][], outputs: number[][][], parameters: {[key: string]: any[]}) {
          this.autoLatencyControl = parameters["autoLatencyControl"][0];
          if (this.array.length === 0 && this.arrays.length === 0) {
            return true;
          }
          const theChannel = 0;
          const output = outputs[0];
          const outputChannel = output[theChannel];

          for (let i = 0; i < outputChannel.length; i++) {
            if (this.arrayOffset >= this.array.length) {
              this.array = this.arrays.shift() || this.emptyArray;
              this.arrayOffset = 0;
            }
            if (this.array.length > 0)
              outputChannel[i] = this.array[this.arrayOffset++];
          }
          return this.running;
        }
      });
    }

    await this.ac.audioWorklet.addModule(`data:text/javascript,(${worklet.toString()})()`);
    this.node = new AudioWorkletNode(this.ac, "audio-feeder");
    this.node.connect(this.gainNode);
  }

  // Give access to these variables inside the worklet definition
  setGainFactor = (gainFactor: number) => {
    this.gainFactor = gainFactor;
  }

  getGainFactor = () => {
    return this.gainFactor;
  };

  setGain = (gain: number) => {
    this.gainNode.gain.value = gain * this.gainFactor;
    if (this.muted)
      this.gainNode.gain.value = 0;
    this.gain = gain;
  }

  getGain = () => {
    return this.gain;
  }

  setMuting(muted: boolean) {
    if (muted)
      this.gainNode.gain.value = 0;
    else {
      this.gainNode.gain.value = this.gain * this.getGainFactor();
    }
    this.muted = muted;
  }

  write(audioData: AudioData) {
    const format = audioData.format;
    let array: Int16Array<ArrayBuffer> | Float32Array<ArrayBuffer> | undefined;

    /* Set up gain factor, for s16 format decoder output, it has to be attenuated by a huge factor!! */
    if (this.gainFactor === 0 && format !== null) {
      this.setGainFactor(format.includes('s16') ? 0.00005 : 1);
      this.setGain(this.getGain());  /* Set to the previously saved gain */
    }

    if (this.emptyArrayMap.get(audioData.numberOfFrames * audioData.numberOfChannels) !== undefined) {
      array = this.emptyArrayMap.get(audioData.numberOfFrames * audioData.numberOfChannels);
    } else if (format !== null) {
      array = format.includes("s16") ?
        new Int16Array(audioData.numberOfFrames * audioData.numberOfChannels)
        :
        new Float32Array(audioData.numberOfFrames * audioData.numberOfChannels);
      this.emptyArrayMap.set(audioData.numberOfFrames * audioData.numberOfChannels, array);
      console.info("New array of " + array.length + " created");
    }
    if (array)
      audioData.copyTo(array, {planeIndex: 0});
    this.node.port.postMessage(array);
    audioData.close();
  }
}

export class AudioStream {
  track: MediaStreamTrack[];
  underlyingSink: MyUnderlyingSink;

  ac: AudioContext;

  constructor(sampleRate: number) {
    const ac = this.ac = new AudioContext({sampleRate: sampleRate, latencyHint: "interactive"});
    const dest = ac.createMediaStreamDestination();
    const track = dest.stream.getAudioTracks();
    // @ts-ignore
    track.writable = new WritableStream(this.underlyingSink = new MyUnderlyingSink(this.ac));
    this.track = track;
  }

  getTrack(): MediaStreamTrack[] {
    return this.track;
  }

  setGain(gain: number) {
    this.underlyingSink.setGain(gain);
  }

  getGain() {
    return this.underlyingSink.getGain();
  }

  setMuting(muted: boolean) {
    this.underlyingSink.setMuting(muted);
  }

  isMuted() {
    return this.underlyingSink.muted;
  }

  setAutoLatencyControl(autoLatencyControl: boolean) {
    const node = this.underlyingSink?.node;
    const context = node?.context;
    if (node && context) {
      node.parameters.get("autoLatencyControl")?.setValueAtTime(autoLatencyControl ? 1 : 0, context.currentTime);
    }
  }

  getAudioLatencyControl() {
    const node = this.underlyingSink?.node;
    if (node) {
      return node.parameters.get("autoLatencyControl")?.value === 1;
    }
    return true;
  }

  async terminate() {
    const node = this.underlyingSink?.node;
    if (node) {
      node.port.postMessage({type: "shutdown"});
      node.disconnect();
      node.port.close();
      await this.ac.close();
      console.log("terminating");
    }
  }
}
