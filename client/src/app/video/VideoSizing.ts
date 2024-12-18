import {Subscription, timer} from "rxjs";

export class VideoSizing {
    el: HTMLVideoElement;

    // To allow for border around video elements
    static readonly bordersEtc: number = 28;
    static readonly navbarTitleAndControls: number = 150;
    static readonly recordingSelectionForm: number = 380;
    static readonly recordingTitleAndControls = 220;

    private _aspectRatio: number = 1;
    private size: number = 100
    private landscapeOnMobile = false;
    private isRecording: boolean = false;

    get aspectRatio(): number {
        return this._aspectRatio
    };

    constructor(el: HTMLVideoElement) {
        this.el = el;
    }

    timerSubscription!: Subscription

    playEventHandler = (ev:Event | null) => {
        this.timerSubscription.unsubscribe();
        this._aspectRatio = this.el.videoHeight / this.el.videoWidth;
        this.setVideoSize(this.size);
    }

    setup(size: number, isRecording: boolean = false) {
        this.isRecording = isRecording;
        this.size = size;
        this.windowResize();
        if(this.el.style.height == "" ) {
            this.el.style.height = "45dvw";  // Prevent flash up of full pixel size image on first access
            this.el.style.width="auto";
        }
        this.timerSubscription = timer(1000).subscribe(() => {
            // Backup for if the play event doesn't fire (Firefox with hevc)
            this.playEventHandler(null);
         });
        this.el.addEventListener('play', this.playEventHandler);
    }

    changeSize(size: number) {
        this.size = size;
        this.setVideoSize(this.size);
    }

    resizeHandler!: (ev: Event) => void;

    //   scrollHandler!: (ev: Event) => void;

    windowResize() {
        this.resizeHandler = (ev: Event) => {
            this.setVideoSize(this.size)
        };
        window.addEventListener('resize', this.resizeHandler);
        // For mobile
    }

    // Increase video size when window is narrower on multi cam display
    adjustVideoSizeForScreen(size: number): number {
        const innerWidth = window.visualViewport == null ? window.innerWidth : window.visualViewport.width;
        return innerWidth < 850 && size < 100 ? 100 :
            innerWidth < 1300 && size < 50 ? 50 :
                innerWidth < 1800 && size < 33.33 ? 33.33 : size;
    }

    private static scrolledToTop = false;
    setVideoSize(size: number) {
        const innerWidth = document.documentElement.clientWidth; // window.visualViewport == null ? window.innerWidth : window.visualViewport.width;
        const innerHeight = document.documentElement.clientHeight;// window.visualViewport == null ? window.innerHeight : window.visualViewport.height;

        //    this.landscape = landscape !== undefined ? landscape : this.landscape;
        let ls = window.matchMedia("screen and (orientation: landscape) and (max-height: 500px)");
        this.landscapeOnMobile = ls.matches;
        const sz = this.adjustVideoSizeForScreen(size);
        if (this.isRecording)
            this.recordingSize(innerHeight, innerWidth)
        else
            this.liveAndMultiSize(innerHeight, innerWidth, sz);
    }

    // Set size of recording playback videos
    recordingSize(innerHeight: number, innerWidth: number) {
        if (!this.landscapeOnMobile) {
            // Fix size on height or width
            if ((innerHeight - VideoSizing.bordersEtc - VideoSizing.recordingTitleAndControls) / this.aspectRatio > innerWidth - VideoSizing.recordingSelectionForm - VideoSizing.bordersEtc) {
                // By width
                let mq = window.matchMedia("screen and (min-width: 686px)");
                if (mq.matches)
                    this.el.style.width = "calc(100dvw - " + (VideoSizing.bordersEtc + VideoSizing.recordingSelectionForm) + "px)";
                else
                    this.el.style.width = "calc(100dvw - " + VideoSizing.bordersEtc + "px)";
                this.el.style.height = "auto";
            } else { // By height
                this.el.style.width = "auto";
                this.el.style.height = "calc(100dvh - " + (VideoSizing.bordersEtc + VideoSizing.recordingTitleAndControls) + "px)";
            }
            VideoSizing.scrolledToTop = false;
        } else {  // Landscape on mobile
            if ((innerHeight - VideoSizing.bordersEtc) / this.aspectRatio > (innerWidth - VideoSizing.bordersEtc)) {
                this.el.style.width = "calc(100dvw - " + (VideoSizing.bordersEtc + VideoSizing.recordingSelectionForm) + "px)";
                this.el.style.height = "auto";
            } else {
                this.el.style.width = "auto";
                this.el.style.height = "calc(100dvh - " + VideoSizing.bordersEtc + "px)";
            }
            if (!VideoSizing.scrolledToTop) {
                // Scroll to fit video in screen if single cam display
                window.scrollTo({left: 0, top: this.el.getBoundingClientRect().y + window.scrollY});
                VideoSizing.scrolledToTop = true;
            }
        }
    }

    // Set size of single and multi live videos
    liveAndMultiSize(innerHeight: number, innerWidth: number, sz: number) {
        if (!this.landscapeOnMobile) {
            // Fix size on height or width
            if ((innerHeight - VideoSizing.bordersEtc - VideoSizing.navbarTitleAndControls) / this.aspectRatio * 100 / sz > innerWidth) {
                // By width
                this.el.style.width = "calc((100dvw - " + (VideoSizing.bordersEtc + "px) / " + 100 / sz + ")");
                this.el.style.height = "auto";
            } else { // By height
                this.el.style.width = "auto";
                this.el.style.height = "calc((100dvh - " + (VideoSizing.bordersEtc + VideoSizing.navbarTitleAndControls) + "px)";
            }
            VideoSizing.scrolledToTop = false;
        } else {  // Landscape on mobile
            if ((innerHeight - VideoSizing.bordersEtc) / this.aspectRatio > (innerWidth)) {
                this.el.style.width = "calc((100dvw - " + VideoSizing.bordersEtc + "px) / " + 100 / sz + ")";
                this.el.style.height = "auto";
            } else {
                this.el.style.width = "auto";
                this.el.style.height = "calc(100dvh - " + VideoSizing.bordersEtc + "px)";
            }
            if (!VideoSizing.scrolledToTop) {
                // Scroll to fit video in screen if single cam display
                window.scrollTo({left: 0, top: this.el.getBoundingClientRect().y + window.scrollY});
                VideoSizing.scrolledToTop = true;
            }
        }
    }

    _destroy() {
        window.removeEventListener('resize', this.resizeHandler)
        this.el.removeEventListener('play', this.playEventHandler);
    }
}
