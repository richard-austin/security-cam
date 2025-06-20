initMSTG = function () {
    if (!window.MediaStreamTrackGenerator) {
        window.MediaStreamTrackGenerator = class MediaStreamTrackGenerator {
          gainFactor = 0;
          gain = 0.5;
          muted = false;
          gainNode;

            constructor({kind, sampleRate}) {
                if (kind === "video") {
                    const canvas = document.createElement("canvas");
                    const ctx = canvas.getContext('2d', {desynchronized: true});
                    const track = canvas.captureStream().getVideoTracks()[0];
                    track.writable = new WritableStream({
                        write(frame) {
                            canvas.width = frame.displayWidth;
                            canvas.height = frame.displayHeight;
                            ctx.drawImage(frame, 0, 0, canvas.width, canvas.height);
                            frame.close();
                        }
                    });
                    return track;
                } else if (kind === "audio") {
                  // Give access to these variables inside the worklet definition
                  let setGainFactor = (gainFactor) => {
                    this.gainFactor = gainFactor;
                  }
                  let getGainFactor = () => {
                    return this.gainFactor
                  };

                  let setGain = (gain) => {
                    this.gainNode.gain.value = gain * this.gainFactor;
                    if(this.muted)
                      this.gainNode.gain.value = 0;
                    this.gain = gain;
                  }

                  let getGain = () => {
                    return this.gain;
                  }

                  const ac = sampleRate ? new AudioContext({sampleRate: sampleRate, latencyHint: 'interactive'}) : new AudioContext({latencyHint: 'interactive'});
                  this.gainNode = ac.createGain()
                  const dest = ac.createMediaStreamDestination();
                    const [track] = dest.stream.getAudioTracks();
                    track.writable = new WritableStream({
                        async start(controller) {
                            this.arrays = [];
                            this.array = [];

                            function worklet() {

                                registerProcessor("mstg-shim", class Processor extends AudioWorkletProcessor {
                                    constructor() {
                                        super();
                                        this.arrays = [];
                                        this.array = [];
                                        this.arrayOffset = 0;
                                        this.port.onmessage = ({data}) => this.arrays.push(data);
                                        this.emptyArray = new Float32Array(0);
                                    }

                                    process(inputs, [[output]]) {
                                      if (this.array.length === 0 && this.arrays.length === 0)
                                        return true;
                                      //console.info("array length = "+this.array.length+": arrays length = "+this.arrays.length+": arrayOffset = "+this.arrayOffset)

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
                            this.node = new AudioWorkletNode(ac, "mstg-shim");
                            this.node.connect(dest);
                            return track;
                        },
                        write(audioData) {
                          const format = audioData.format;
                          let array;

                          // Set up gain factor, for s16 format decoder output, it has to be attenuated by a huge factor!!
                          if (getGainFactor() === 0) {
                            setGainFactor(format.includes('s16') ? 0.00005 : 1);
                            setGain(getGain());  // Set to the previously saved gain
                          }

                          if(this.arrays[audioData.numberOfFrames * audioData.numberOfChannels] !== undefined) {
                            array = this.arrays[audioData.numberOfFrames * audioData.numberOfChannels];
                          } else {
                            array = format.includes("s16") ?
                              new Int16Array(audioData.numberOfFrames * audioData.numberOfChannels)
                              :
                              new Float32Array(audioData.numberOfFrames * audioData.numberOfChannels);
                            this.arrays[audioData.numberOfFrames * audioData.numberOfChannels] = array
                            console.info("New array of "+array.length+" created");
                          }


                          audioData.copyTo(array, {planeIndex: 0});
                          // console.info("Packet format: "+audioData.format);
                          this.node.port.postMessage(array);
                          audioData.close();
                        }
                    });
                    return track;
                }
            }
        };
    }
}
