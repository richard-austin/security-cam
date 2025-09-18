//
// Notes:-
// This is derived from the MediaStreamTrackGenerator polyfill used for browsers not supporting MediaStreamTrackGenerator.
// The polyfill as originally obtained doesn't work properly with audio as the AudioContext seems to default to sample rate
// 48000. This is fixed here by passing the correct sample rate to the AudioContext constructor. There is some audio latency drift
// when the media stream track is passed to MediaStream for output to a media element. This is fixed here by passing the
// audio worklet node output to the audio context destination via a gain node. As this is used on Chrome browsers now,
// the audioData format in the write function (from the decoder) must be checked as it is Int16 with G711 audio, and
// Float32 with AAC, and the correct array type must be set up. Additionally a very large amount of attenuation is
// required (by setting the gain value) for Int16.
//
class AudioStream {
  gainFactor = 0;
  gain = 0.5;
  muted = false;
  gainNode;
  track;

  constructor(sampleRate) {
    const ac = new AudioContext({sampleRate: sampleRate, latencyHint: "interactive"});
    this.gainNode = ac.createGain()
    this.gainNode.connect(ac.destination);

    // Give access to these variables inside the worklet definition
    let setGainFactor = (gainFactor) => {
      this.gainFactor = gainFactor;
    }
    let getGainFactor = () => {
      return this.gainFactor
    };

    let setGain = (gain) => {
      this.gainNode.gain.value = gain * this.gainFactor;
      if (this.muted)
        this.gainNode.gain.value = 0;
      this.gain = gain;
    }

    let getGain = () => {
      return this.gain;
    }

    const dest = ac.createMediaStreamDestination();
    const [track] = dest.stream.getAudioTracks();
    const gainNode = this.gainNode;
    track.writable = new WritableStream({
      async start(controller) {
        this.emptyArrayMap = new Map();

        function worklet() {
          registerProcessor("audio-feeder", class Processor extends AudioWorkletProcessor {
            constructor() {
              super();
              // BufferStats class must be declared here as it is instantiated in the AudioContext
              this.bs = new class BufferStats {
                arraySize = 20;
                statsArray = new ArrayBuffer(this.arraySize);
                arrayIndex = 0;
                fullArray = false;
                updated = false;
                lastAverage = 0;
                sdUpdated = false;
                lastSd = 0;
                startTime = currentTime;
                measurementWindowSecs = 20;
                measuring = true;
                maxBufferSize = 20;

                update(val) {
                  if (this.measuring) {
                    this.updated = this.sdUpdated = true;
                    this.statsArray[this.arrayIndex++] = val;
                    if (!this.fullArray && this.arrayIndex === this.arraySize)
                      this.fullArray = true;
                    if (this.arrayIndex >= this.arraySize)
                      this.arrayIndex = 0;
                    const timeNow = currentTime;
                    if (timeNow - this.startTime > this.measurementWindowSecs) {
                      if (this.fullArray) {
                        this.maxBufferSize = Math.round(this.average() + 2 + this.standardDeviation());
                        this.startTime = timeNow;
                        this.measuring = false;
                        console.debug("maxBufferSize set to ", this.maxBufferSize);
                      } else {
                        console.error("Moving average array not full, cannot calculate bufferSize");
                      }
                    }
                  }
                }

                average() {
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

                standardDeviation() {
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

                reset() {
                  this.arrayIndex = 0;
                  this.fullArray = false;
                  this.updated = false;
                  this.lastAverage = 0;
                  this.sdUpdated = false;
                  this.lastSd = 0;
                  this.startTime = currentTime;
                  this.measuring = true;
                }

                bufferTooLarge(bufferSize) {
                  const bufferTooLarge = !this.measuring && bufferSize > this.maxBufferSize;
                  if (bufferTooLarge)
                    this.reset();
                  return bufferTooLarge;
                }
              };

              this.arrays = [];
              this.array = [];
              this.arrayOffset = 0;
              this.port.onmessage = ({data}) => {
                this.arrays.push(data);
                this.bs.update(this.arrays.length);
                // // Prevent audio latency build up due to delayed packets etc.
                if (this.bs.bufferTooLarge(this.arrays.length)) {
                  console.debug("Reducing audio packets queue from " + this.arrays.length + " to 1");
                  this.arrays = this.arrays.slice(this.arrays.length - 1);
                }
              }
              this.emptyArray = new Float32Array(0);
            }

            // Audio worklet processor function
            process(inputs, [[output]]) {
              if (this.array.length === 0 && this.arrays.length === 0)
                return true;
              for (let i = 0; i < output.length; i++) {
                if (this.arrayOffset >= this.array.length) {
                  this.array = this.arrays.shift() || this.emptyArray;
                  this.arrayOffset = 0;
                }
                if (this.array.length > 0)
                  output[i] = this.array[this.arrayOffset++] || 0;
              }
              return true;
            }
          });
        }

        await ac.audioWorklet.addModule(`data:text/javascript,(${worklet.toString()})()`);
        this.node = new AudioWorkletNode(ac, "audio-feeder");
        this.node.connect(gainNode);
      },
      write(audioData) {
        const format = audioData.format;
        let array;

        // Set up gain factor, for s16 format decoder output, it has to be attenuated by a huge factor!!
        if (getGainFactor() === 0) {
          setGainFactor(format.includes('s16') ? 0.00005 : 1);
          setGain(getGain());  // Set to the previously saved gain
        }

        if (this.emptyArrayMap[audioData.numberOfFrames * audioData.numberOfChannels] !== undefined) {
          array = this.emptyArrayMap[audioData.numberOfFrames * audioData.numberOfChannels];
        } else {
          array = format.includes("s16") ?
            new Int16Array(audioData.numberOfFrames * audioData.numberOfChannels)
            :
            new Float32Array(audioData.numberOfFrames * audioData.numberOfChannels);
          this.emptyArrayMap[audioData.numberOfFrames * audioData.numberOfChannels] = array
          console.info("New array of " + array.length + " created");
        }


        audioData.copyTo(array, {planeIndex: 0});
        this.node.port.postMessage(array);
        audioData.close();
      }
    });
    this.track = track;
    // this.audioFeed = new AudioFeeder(url, track)
  }

  getTrack() {
    return this.track;
  }

  setGain(gain) {
    this.gainNode.gain.value = gain * this.gainFactor;
    this.gain = gain;
  }

  getGain() {
    return !this.muted ? this.gain : 0;
  }

  setMuting(muted) {
    if (muted)
      this.gainNode.gain.value = 0;
    else {
      this.gainNode.gain.value = this.gain * this.gainFactor;
    }
    this.muted = muted;
  }

  isMuted() {
    return this.muted;
  }
}
