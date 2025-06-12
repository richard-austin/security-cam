class AudioStream {
    gainFactor = 0;
    gain = 0.5;
    muted = false;
    gainNode;

    constructor(sampleRate, url) {
        const ac = new AudioContext({sampleRate: sampleRate});
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
            this.gain = this.gainNode.gain.value = gain * this.gainFactor;
            if(this.muted)
                this.gainNode.gain.value = 0;
        }

        const dest = ac.createMediaStreamDestination();
        const [track] = dest.stream.getAudioTracks();
        const gainNode = this.gainNode;
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
                            if (this.array.length === 0 && this.arrays.length === 0)
                                return true;
                            //   console.info("array length = "+this.array.length+": arrays length = "+this.arrays.length+": arrayOffset = "+this.arrayOffset)

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
                    setGainFactor(format === 's16' ? 0.00005 : 1);
                    setGain(0.5);
                }

                array = format === "s16" ?
                    new Int16Array(audioData.numberOfFrames * audioData.numberOfChannels)
                    :
                    new Float32Array(audioData.numberOfFrames * audioData.numberOfChannels)
                this.arrays[audioData.numberOfFrames] = array

                audioData.copyTo(array, {planeIndex: 0});
                // console.info("Packet format: "+audioData.format);
                this.node.port.postMessage(array, [array.buffer]);
                audioData.close();
            }
        });
        this.audioFeed = new AudioFeeder(url, track)
    }

    setGain(gain) {
        this.gain = this.gainNode.gain.value = gain * this.gainFactor;
    }

    getGain() {
        return this.gainNode.gain.value / this.gainFactor;
    }

    setMuting(muted) {
        if (muted)
            this.gainNode.gain.value = 0;
        else {
            this.gainNode.gain.value = this.gain;
        }
        this.muted = muted;

        console.info("Muted: "+this.muted+" Gain: "+this.gainNode.gain.value);
    }

    isMuted() {
        return this.muted;
    }

    close() {
        this.audioFeed.close();
    }
}


class AudioFeeder {
    config = {
        numberOfChannels: 1,
        sampleRate: 8000,  // Firefox hard codes to 48000
        codec: 'alaw',
    };

    timeout;
    ws;
    noRestart = false;
    started = false;
    url = ""
    writer;

    constructor(url, track) {
        console.info("In AudioFeeder constructor, track = " + track)
        this.url = url;
        console.info("writable = " + track.writable);
        this.writer = track.writable.getWriter();
        console.info("writer = " + this.writer);
        this.setUpWSConnection();
    }

    audioDecoder = new AudioDecoder({
        output: async (frame) => {
            this.writer.write(frame);
            //frame.close();
        },
        error: (e) => {
            console.warn("Audio decoder: " + e.message);
        },
    });

    setUpWSConnection() {
        console.info("In setUpWSConnection")
        this.ws = new WebSocket(this.url);
        this.ws.binaryType = 'arraybuffer';

        this.ws.onerror = (ev) => {
            postMessage({warningMessage: "An error occurred with the audio feeder websocket connection"})
        }

        this.ws.onclose = (ev) => {
            if (this.noRestart)
                postMessage({closed: true})
            console.warn("The audio feed websocket was closed: " + ev.reason);
            // clearTimeout(this.timeout);
        }

        this.timeout = setTimeout(() => {
            this.timedOut();
        }, 6000);

        this.ws.onmessage = async (event) => {
            if (!this.started) {
                let array = new Uint8Array(event.data)
                if (array[0] === 9) {
                    let decoded_arr = array.slice(1);
                    let audioInfo = JSON.parse(this.Utf8ArrayToStr(decoded_arr));
                    this.config.codec = audioInfo.codec_name === "aac" ? "mp4a.40.2" : "alaw";
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

    timedOut() {
        if (!this.closed) {
            console.error("Audio feed from websocket has stopped...");
            if (this.ws)
                this.ws.close();
            if (this.audioDecoder)
                this.audioDecoder.close();
        }
    }

    close() {
        this.closed = true;
        this.noRestart = true;
        if (this.audioDecoder) {
            this.audioDecoder.close();
            this.audioDecoder = undefined;
        }
        if (this.ws)
            this.ws.close();
    }

    Utf8ArrayToStr(array) {
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

