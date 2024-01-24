import {Subscription, timer} from "rxjs";

export class MouseWheelZoom {
  private originX: number = 0;
  private originY: number = 0;
  private newOriginX: number = 0;
  private newOriginY: number = 0;
  private readonly maxScale: number = 6;
  private readonly minScale: number = 1;
  private readonly video: HTMLVideoElement;
  private readonly div: HTMLDivElement;
  private scale: number = this.minScale;
  private touchStartDist!: number;  // Distance aprt of fingers at the touchStart
  readonly fiddleFactor: number = 5;  // Multiplier to get the best pinch zoom response
  private deltaX: number = 0;
  private deltaY: number = 0;
  private prevDeltaX: number = 0;
  private prevDeltaY: number = 0;
  private prevSc: number = this.minScale;

  constructor(video: HTMLVideoElement, div: HTMLDivElement) {
    this.video = video;
    this.div = div;
  }

  /**
   * mouseWheel: Zoom video around the mouse position by rolling the mouse wheel
   * @param ev The wheel event
   */
  public mouseWheel(ev: WheelEvent) {

    // Get the bounding rectangle of target
    const rect = this.div.getBoundingClientRect();
    const deltaWheel = -ev.deltaY / 400;
    const prevScale = this.scale;
    const x1 = ev.clientX - rect.left;
    const y1 = ev.clientY - rect.top;
    if (this.scale == this.minScale) {
      this.originX = this.newOriginX = x1;
      this.originY = this.newOriginY = y1;
      this.deltaX = this.deltaY = this.prevDeltaX = this.prevDeltaY = 0;
    }
    if (deltaWheel > 0 && this.scale < this.maxScale) {
      this.scale += deltaWheel;
      this.scale = Math.min(this.scale, this.maxScale);
    } else if (deltaWheel < 0 && this.scale > this.minScale) {
      this.scale += deltaWheel;
      this.scale = Math.max(this.scale, this.minScale);
    }

    if (this.scale > this.minScale) {
      if (x1 !== this.newOriginX || y1 !== this.newOriginY) {
        this.prevSc = prevScale;
        this.newOriginX = x1;
        this.prevDeltaX = this.deltaX;
        this.newOriginY = y1;
        this.prevDeltaY = this.deltaY;
      }
      this.deltaX = (this.prevDeltaX*this.scale-(this.newOriginX - this.originX) * (this.scale - this.prevSc))/this.prevSc;
      this.deltaY = (this.prevDeltaY*this.scale-(this.newOriginY - this.originY) * (this.scale - this.prevSc))/this.prevSc;
    }
    console.log("originX = "+this.originX+" newOriginX = "+this.newOriginX+" originY = "+this.originY+" newOriginY = "+this.newOriginY+" scale = "+this.scale+" prevSc = "+this.prevSc+" deltaX = "+this.deltaX+" deltaY = "+this.deltaY)
    this.transform();

    ev.preventDefault();
    // log("x = "+this.transformOriginX+" y = "+this.transformOriginY);
    // log("Scale = "+this.scale);
  }

  bMouseDown: boolean = false;
  bDragging: boolean = false;

  mouseDown(ev: MouseEvent) {
    this.bMouseDown = true;

    console.log("mouseDown: clientX = " + ev.clientX + " clientY = " + ev.clientY);
  }

  mouseMove(ev: MouseEvent) {
    if (this.bMouseDown) {
      this.bDragging = true;
      this.transform();
      console.log("mouseMove: clientX = " + ev.clientX + " clientY = " + ev.clientY);
    }
  }

  mouseUp(ev: MouseEvent) {
    if (this.bDragging)
    this.bMouseDown = this.bDragging = false;
    console.log("mouseMove: clientX = " + ev.clientX + " clientY = " + ev.clientY);
  }

  private zoom(transitionDuration: number = 550): void {
      this.video.style.transformOrigin = (this.originX) + "px " + (this.originY) + "px";
      this.video.style.transitionProperty = "transform";
      this.video.style.transitionDuration = transitionDuration + "ms";
      this.video.style.transform = "translate(" + this.deltaX + "px, " + this.deltaY + "px) scale(" + this.scale + ")";
      //     this.video.style.transform = "scale(" + this.scale + ") translate(" + this.xDist + "px, " + this.yDist + "px";
  }

  private transform() {
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = 0 + "ms";
    this.video.style.transformOrigin = (this.originX) + "px " + (this.originY) + "px";
    //   this.fixWithinViewPort();
    this.video.style.transform = "translate(" + this.deltaX + "px, " + this.deltaY + "px) scale(" + this.scale + ")";
//    console.log(this.xDist + " : " + this.yDist);
  }

  private fixWithinViewPort() {
    const rect: DOMRect = this.div.getBoundingClientRect();
    const width = rect.width - this.div.clientLeft;
    const height = rect.height - this.div.clientTop;
    if (this.deltaX + this.originX * (1 - this.scale) > 0)
      this.deltaX = -this.originX * (1 - this.scale)
    else if (this.deltaX + this.originX + (width - this.originX) * this.scale < width)
      this.deltaX = -(width - this.originX) * this.scale - this.originX + width;

    if (this.deltaY + this.originY * (1 - this.scale) > 0)
      this.deltaY = -this.originY * (1 - this.scale);
    else if (this.deltaY + this.originY + (height - this.originY) * this.scale < height)
      this.deltaY = -(height - this.originY) * this.scale - this.originY + height;
  }

  reset(slow: boolean = false): void {
    this.scale = this.prevSc = this.minScale;
    this.originX = this.newOriginX = this.originY = this.newOriginY = 0;
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = slow ? "200ms" : "0ms";
    this.video.style.transform = "scale(" + this.minScale + ")";
    this.deltaX = this.deltaY = 0;
  }

  private tapTimer: Subscription | null = null;
  private currentTouches: number = 0;

  touchStartHandler(ev: TouchEvent) {
    console.log("touchStartHandler: touches=" + ev.touches.length);
    this.currentTouches = ev.touches.length;
    const rect = this.div.getBoundingClientRect();
    if (ev.touches.length == 2) {
      if (this.scale === this.minScale) {
        this.originX = ((ev.touches[0].clientX + ev.touches[1].clientX) / 2 - rect.left) * this.scale;
        this.originY = ((ev.touches[0].clientY + ev.touches[1].clientY) / 2 - rect.top) * this.scale;
      }
      this.touchStartDist = this.pythagoras(ev);
    } else if (ev.touches.length === 1) { // Handle double tap to reset zoom, or single to shift the zoomed image.
      if (this.tapTimer == null) {  // 1 tap
        this.tapTimer = timer(300).subscribe(() => {
          this.tapTimer?.unsubscribe();
          this.tapTimer = null;
        });
      } else {  // 2 taps
        this.tapTimer.unsubscribe();
        this.tapTimer = null;
        this.reset(true);
      }
    }
//    console.log("transformOriginX: "+this.transformOriginX+" transformOriginY: "+this.transformOriginY);
  }

  touchMoveHandler(ev: TouchEvent) {
    console.log("touchMoveHandler: touches=" + ev.touches.length);
    const rect: DOMRect = this.div.getBoundingClientRect();
    if (ev.touches.length == 2 && this.currentTouches === ev.touches.length) {
      const dist: number = this.pythagoras(ev);
      const distChange: number = dist - this.touchStartDist;
      this.touchStartDist = dist;
      // Calculate new scale value, keeping it withing the limits minScale - maxScale
   //   this.scale = this.prevScale + distChange / rect.width * this.fiddleFactor;
      this.scale = Math.min(this.scale, this.maxScale);
      this.scale = Math.max(this.minScale, this.scale);
//      console.log("scale: "+this.scale);
      this.zoom(100);
    } else if (ev.touches.length === 1 && this.currentTouches === ev.touches.length) {
      const newX: number = ev.touches[0].clientX;
      const newY: number = ev.touches[0].clientY;
      this.transform();
    }
    if (this.scale !== this.minScale)
      ev.preventDefault();  // Allow touchMove default action if no zoom to allow scrolling of multicam page on smartphone
  }

  touchEndHandler(ev: TouchEvent) {
    console.log("touchEndHandler: touches=" + ev.touches.length);
    this.currentTouches = ev.touches.length == 0 ? 0 : this.currentTouches;
//    this.prevScale = this.scale;
    ev.preventDefault()
  }

  pythagoras(ev: TouchEvent): number {
    if (ev.touches.length === 2)
      return Math.sqrt((ev.touches[0].clientX - ev.touches[1].clientX) ** 2 +
        (ev.touches[0].clientY - ev.touches[1].clientY) ** 2);
    else
      return 0;
  }
}
