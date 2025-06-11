initAudioStream = function() {
    if(!window.AudioStream) {
        window.AudioStream = class AudioStream {
            constructor(sampleRate) {
                const ac = new AudioContext({sampleRate: sampleRate});
                const g = ac.createGain()
                g.gain.value = 0.0001;
                g.connect(ac.destination);

                const dest = ac.createMediaStreamDestination();
                const [track] = dest.stream.getAudioTracks();
                track.writable = new WritableStream({
                    async start(controller) {
                        this.arrays = [];
                        function worklet() {
                            registerProcessor("audio-feeder", class Processor extends AudioWorkletProcessor {
                                constructor() {
                                    super();
                                    this.arrays = [];
                                    this.array = [];
                                    this.arrayOffset = 0;
                                    this.port.onmessage = ({data}) => this.arrays.push(data);
                                    this.emptyArray = new Float32Array(0);
                                }

                                process(inputs, [[output]]) {
                                    if(this.array.length === 0 && this.arrays.length === 0)
                                        return true;
                                 //   console.info("array length = "+this.array.length+": arrays length = "+this.arrays.length+": arrayOffset = "+this.arrayOffset)

                                    for (let i = 0; i < output.length; i++) {
                                        if (this.arrayOffset >= this.array.length) {
                                            this.array = this.arrays.shift() || this.emptyArray;
                                            this.arrayOffset = 0;
                                        }
                                        if(this.array.length > 0)
                                            output[i] = this.array[this.arrayOffset++] || 0;
                                    }
                                    return true;
                                }
                            });
                        }

                        await ac.audioWorklet.addModule(`data:text/javascript,(${worklet.toString()})()`);
                        this.node = new AudioWorkletNode(ac, "audio-feeder");
                        this.node.connect(g);
                        //this.node.connect(ac.destination);
                        return track;
                    },
                    write(audioData) {
                        const format = audioData.format;
                        g.gain.value = format === 's16' ? 0.0001 : 1; // Who knew?
                        const array = format === "s16" ?
                            new Int16Array(audioData.numberOfFrames * audioData.numberOfChannels)
                            :
                            new Float32Array(audioData.numberOfFrames * audioData.numberOfChannels)

                        audioData.copyTo(array, {planeIndex: 0});
                        console.info("Packet format: "+audioData.format);
                        this.node.port.postMessage(array, [array.buffer]);
                        audioData.close();
                    }
                });
                return track;
            }
        }
    }

    // config = {
    //     numberOfChannels: 1,
    //     sampleRate: 8000,  // Firefox hard codes to 48000
    //     codec: 'alaw',
    // };
    //
    // timeout;
    // ws;
    // noRestart = false;
    // started = false;
    //
    // constructor() {
    //     super();
    //     this.port.postMessage("Worklet Running");
    //     this.setUpWSConnection();
    //     this.port.onMessage = () => {
    //     }
    // }
    //
    // process(input, output, parameters) {
    //     return true;
    // }
    //
    // setUpWSConnection() {
    //     this.ws = new WebSocket(this.url);
    //     this.ws.binaryType = 'arraybuffer';
    //
    //     this.ws.onerror = (ev) => {
    //         postMessage({warningMessage: "An error occurred with the audio feeder websocket connection"})
    //     }
    //
    //     this.ws.onclose = (ev) => {
    //         if (this.noRestart)
    //             postMessage({closed: true})
    //         console.warn("The audio feed websocket was closed: " + ev.reason);
    //         // clearTimeout(this.timeout);
    //     }
    //
    //     this.timeout = setTimeout(() => {
    //         this.timedOut();
    //     }, 6000);
    //
    //     this.ws.onmessage = async (event) => {
    //         if (!this.started) {
    //             let array = new Uint8Array(event.data)
    //             if (array[0] === 9) {
    //                 let decoded_arr = array.slice(1);
    //                 let audioInfo = JSON.parse(this.Utf8ArrayToStr(decoded_arr));
    //                 this.config.codec = audioInfo.codec_name === "aac" ? "mp4a.40.2" : "alaw";
    //                 this.config.sampleRate = parseInt(audioInfo.sample_rate);
    //                 this.audioDecoder.configure(this.config);
    //                 console.log('first audio packet with codec data: ' + this.config.codec);
    //                 this.started = true;
    //             } else
    //                 console.error("No audio codec was found")
    //         } else {
    //             // @ts-ignore
    //             const eac = new EncodedAudioChunk({
    //                 type: 'key',
    //                 timestamp: 0,
    //                 duration: 1,
    //                 data: event.data,
    //             });
    //             await this.audioDecoder.decode(eac)
    //         }
    //         this.resetTimeout();
    //     };
    // }
    //
    // resetTimeout = () => {
    //     clearTimeout(this.timeout);
    //     this.timeout = setTimeout(() => {
    //         this.timedOut();
    //     }, 3000)
    // }
    //
    // timedOut() {
    //     console.error("Audio feed from websocket has stopped...");
    //     if (this.ws)
    //         this.ws.close();
    //     if (this.audioDecoder)
    //         this.audioDecoder.close();
    // }
    //
    // close() {
    //     this.noRestart = true;
    //     if (this.audioDecoder)
    //         this.audioDecoder.close();
    //     if (this.ws)
    //         this.ws.close();
    // }
    //
    // Utf8ArrayToStr(array) {
    //     let out, i, len;
    //     out = '';
    //     len = array.length;
    //     i = 0;
    //     while (i < len) {
    //         out += String.fromCharCode(array[i]);
    //         ++i;
    //     }
    //     return out;
    // }
}

