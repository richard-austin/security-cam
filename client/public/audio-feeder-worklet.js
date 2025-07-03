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
            if(this.muted)
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
