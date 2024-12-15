export class VideoSizing {
    el: HTMLVideoElement;

    // To allow for border around video elements
    static readonly bordersEtc: number = 28;
    static readonly navbarTitleAndControls: number = 150;

    private _aspectRatio: number = 1;
    private size: number = 100
    private landscapeOnMobile = false;
    private multi: boolean = false;

    get aspectRatio(): number {
        return this._aspectRatio
    };

    constructor(el: HTMLVideoElement) {
        this.el = el;
    }

    setup(size: number, multi: boolean) {
        this.size = size;
        this.multi = multi;
        this.windowResize();
        if(this.el.style.height == "" ) {
            this.el.style.height = "45dvw";  // Prevent flash up of full pixel size image on first access
            this.el.style.width="auto";
        }
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
        // For mobile
    }

    // Increase video size when window is narrower on multi cam display
    adjustVideoSizeForScreen(size: number): number {
        const innerWidth = window.visualViewport == null ? window.innerWidth : window.visualViewport.width;
        return innerWidth < 850 && size < 100 ? 100 :
            innerWidth < 1300 && size < 50 ? 50 :
                innerWidth < 1800 && size < 33.33 ? 33.33 : size;
    }

    setVideoSize(size: number) {
        const innerWidth = document.documentElement.clientWidth; // window.visualViewport == null ? window.innerWidth : window.visualViewport.width;
        const innerHeight = document.documentElement.clientHeight;// window.visualViewport == null ? window.innerHeight : window.visualViewport.height;

        //    this.landscape = landscape !== undefined ? landscape : this.landscape;
        let ls = window.matchMedia("screen and (orientation: landscape) and (max-height: 500px)");
        this.landscapeOnMobile = ls.matches;
        const sz = this.adjustVideoSizeForScreen(size);
        if (!this.landscapeOnMobile) {
            // Fix size on height or width
            if ((innerHeight - VideoSizing.bordersEtc - VideoSizing.navbarTitleAndControls) / this.aspectRatio * 100 / sz > innerWidth) {
                // By width
                this.el.style.width = "calc((100dvw - " + VideoSizing.bordersEtc + "px) / " + 100 / sz + ")";
                this.el.style.height = "auto";
            } else { // By height
                this.el.style.width = "auto";
                this.el.style.height = "calc((100dvh - " + (VideoSizing.bordersEtc + VideoSizing.navbarTitleAndControls) + "px)";
            }
        } else {  // Landscape on mobile
            if ((innerHeight - VideoSizing.bordersEtc) / this.aspectRatio > (innerWidth)) {
                this.el.style.width = "calc((100dvw - " + VideoSizing.bordersEtc + "px) / " + 100 / sz + ")";
                this.el.style.height = "auto";
            } else {
                this.el.style.width = "auto";
                this.el.style.height = "calc(100dvh - " + VideoSizing.bordersEtc + "px)";
            }
            // Scroll to fit video in screen if single cam display
            if(!this.multi)
                window.scrollTo({left: 0, top: this.el.getBoundingClientRect().y + window.scrollY});
        }
    }

    _destroy() {
        window.removeEventListener('resize', this.resizeHandler)
        this.el.onplaying = null;
    }
}
