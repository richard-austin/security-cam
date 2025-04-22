/// <reference lib="webworker" />

let videoFeeder!:VideoFeeder;
addEventListener('message', ({data}) => {
    if(data.url) {
        videoFeeder = new VideoFeeder(data.url)
        videoFeeder.setUpWebsocketConnection();
        videoFeeder.setupDecoder();
    }
    else if(data.close && videoFeeder) {
        videoFeeder.close()
    }
});

class VideoFeeder {
    private decoder!: VideoDecoder;
    private firstKeyFrameReceived: boolean = false;
    private buffer = new Uint8Array();

    private largeBuffers: Uint8Array[] = [];
    private bufferInUse = false;
    private timeout!: NodeJS.Timeout;
    private readonly url!: string;
    private ws!: WebSocket;
    private noRestart: boolean = false;
    private isHEVC: boolean = false;
    private started = false;
    private codec = ""; // https://wiki.whatwg.org/wiki/Video_type_parameters

    constructor(url: string) {
        this.url = url;
    }

    setupDecoder(): void {
        this.firstKeyFrameReceived = false;
        this.decoder = new VideoDecoder({
            output: async (frame) => {
                postMessage(frame);
                frame.close();
            },
            error: (e) => {
                console.warn("Video decoder:" + e.message);
                this.setupDecoder();
            },
        });
    }

    processMessage = async (data: Uint8Array): Promise<void> => {
        // let processStart = performance.now();
        if (this.decoder.state !== "configured") {
            const config = {codec: this.codec, optimizeForLatency: true};
            this.decoder.configure(config);
        }
        const chunk = new EncodedVideoChunk({
            timestamp: (performance.now()) * 1000,
            type: (this.isHEVC ? (data[3] === 0x40) : ((data[4] & 0x0f) === 7)) ? "key" : "delta",
            data: data,
        });
        this.decoder.decode(chunk);
        // console.log("process time: "+(performance.now()-processStart))
    }

    setUpWebsocketConnection() {
        this.isHEVC = false;
        this.ws = new WebSocket(this.url);
        this.ws.binaryType = 'arraybuffer';
        this.ws.onmessage = (event: MessageEvent) => {
            if (!this.started) {
                let array = new Uint8Array(event.data)
                if (array[0] === 9) {
                    let decoded_arr = array.slice(1);
                    this.codec = this.Utf8ArrayToStr(decoded_arr);
                    console.log('first packet with codec data: ' + this.codec);
                    this.started = true;
                } else
                    console.error("No codec was found")
            } else {
                this.processChunk(event.data).then();
            }
        };

        this.ws.onerror = (ev) => {
            console.error("An error occurred with the video feeder websocket connection")
        }

        this.ws.onclose = (ev) => {
            if(this.noRestart)
                postMessage({closed: true})
            console.info("The video feed websocket was closed: " + ev.reason)
        }

        this.timeout = setTimeout(() => {
            this.timeoutRestart();
        }, 6000)

    }
    close() {
        this.noRestart = true;
        this.ws.close()
    }
    resetTimeout() {
        clearTimeout(this.timeout);
        this.timeout = setTimeout(() => {
            this.timeoutRestart();
        }, 6000)
    }

    timeoutRestart() {
        console.error("Video feed from websocket has stopped, restarting ...");
        if(this.ws)
            this.ws.close();
        setTimeout(() => {
            this.setUpWebsocketConnection();
        }, 1000)
    }

    async putLargeFrames(): Promise<void> {
        if (this.largeBuffers.length > 0) {
            let totalLength = 0;
            this.largeBuffers.forEach((element: Uint8Array) => {
                totalLength += element.length;
            });
            let offset = 0;
            const mergedArray = new Uint8Array(totalLength);
            this.largeBuffers.forEach((element: Uint8Array) => {
                mergedArray.set(element, offset);
                offset += element.length;
            });
            this.largeBuffers = [];
            await this.processMessage(mergedArray);
        }
    }

    readonly hevcStart:Uint8Array = new Uint8Array([0x00,0x00,0x01]);
    readonly h264Start:Uint8Array = new Uint8Array([0x00,0x00,0x00,0x01]);
    readonly h264KeyFrame1:Uint8Array = new Uint8Array([0x67,0x64]);
    readonly h264KeyFrame2:Uint8Array = new Uint8Array([0x27,0x64]);
    readonly h264KeyFrame3:Uint8Array = new Uint8Array([0x61,0x88]);

    isStartFrame(buffer:Uint8Array):boolean {
        let startFrame: boolean = this.h264Start.every((value, index) => value === buffer[index]);
        if(!startFrame) {// Not h264, try hevc
            startFrame = this.hevcStart.every((value, index) => value === buffer[index]);
            this.isHEVC = startFrame;
        }
        return startFrame;
    }

    /**
     * isKeyFrame: Detects h264 and hevc keyframes. isStartFrame must be called before the first use of this.
     * @param buffer Encoded h264 or hevc video block.
     */
    isKeyFrame(buffer:Uint8Array):boolean {
        if(this.isHEVC) {
            const byte = (buffer[3] >> 1) & 0x3f;
            return byte === 0x19 || byte === 0x20;
        } else {
            let isKeyFrame = this.h264KeyFrame1.every((value, index) => value === buffer[index+4]);
            if(!isKeyFrame)
                isKeyFrame = this.h264KeyFrame2.every((value, index) => value === buffer[index+4]);
            if(!isKeyFrame)
                isKeyFrame = this.h264KeyFrame3.every((value, index) => value === buffer[index+4]);
            return isKeyFrame;
        }
    }

    async processChunk(value: ArrayBuffer): Promise<void> {
        //  let processChunkStart = performance.now();
        this.resetTimeout();
        this.buffer = new Uint8Array(value);
        const isStartFrame = this.isStartFrame(this.buffer);

        const isKeyFrame = this.isKeyFrame(this.buffer);

        if (isKeyFrame)
            this.firstKeyFrameReceived = true;
        if (this.firstKeyFrameReceived) {
            if (isStartFrame && this.bufferInUse) {
                await this.putLargeFrames();
                this.bufferInUse = false;
            }
            // If the length is the maximum packet size, put into the large buffers array to append
            //  together for processing by the decoder
            if (value.byteLength >= 32767)
                /**
                    TODO: Need to find the actual packet length as this will fail if a camera has a
                       shorter max packet length than 32K, or if multiple packets are conjoined.
                 **/
                this.bufferInUse = true;
            if (this.bufferInUse) {
                this.largeBuffers.push(this.buffer);
            } else if (isStartFrame)
                await this.processMessage(this.buffer);
            else
                console.error("Video processing: malformed message received")
        }
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
