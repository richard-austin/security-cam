export class VideoSizing {
    el: HTMLVideoElement;

    // To allow for border around video elements
    static readonly bordersEtc: number = 12;
    static readonly navbarTitleAndControls: number = 150;

    private _aspectRatio: number = 0;
    private size: number = 100

    get aspectRatio(): number {
        return this._aspectRatio
    };

    constructor(el: HTMLVideoElement) {
        this.el = el;
    }

    setup(size: number) {
        this.size = size;
        this.windowResize();
        this.el.onplaying = ev => {
            this._aspectRatio = this.el.videoHeight / this.el.videoWidth;
            this.setVideoSize(this.size);
        }
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
    }

    // Increase video size when window is narrower on multi cam display
    adjustVideoSizeForScreen(size: number): number {
        return window.innerWidth < 850 && size < 100 ? 100 :
            window.innerWidth < 1300 && size < 50 ? 50 :
                window.innerWidth < 1800 && size < 33.33 ? 33.33 : size;
    }

    setVideoSize(size: number) {
        const sz = this.adjustVideoSizeForScreen(size);
        let width = sz / 100 * window.innerWidth - VideoSizing.bordersEtc;
        let height = width * this.aspectRatio;
        const maxHeight = window.innerHeight - VideoSizing.bordersEtc - VideoSizing.navbarTitleAndControls;
        if (height > maxHeight) {
            width /= height / maxHeight;
        }
        this.el.style.width = width + "px";
        this.el.style.height = "auto"
        console.log("Changing size to " + size + " => " + this.el.style.width + " x " + this.el.style.height)
    }

    _destroy() {
        window.removeEventListener('resize', this.resizeHandler)
        this.el.onplaying = null;
    }
}
