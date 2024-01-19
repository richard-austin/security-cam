import {Subscription, timer} from "rxjs";

export class MouseWheelZoom {
  private transformOriginX: number = 0;
  private transformOriginY: number = 0;
  private readonly maxScale: number = 6;
  private readonly minScale: number = 1;
  private readonly video: HTMLVideoElement;
  private readonly div: HTMLDivElement;
  private scale: number = this.minScale;
  private prevScale: number = this.minScale;
  private touchStartDist!: number;  // Distance aprt of fingers at the touchStart
  readonly fiddleFactor: number = 5;  // Multiplier to get the best pinch zoom response
  private translateOriginX: number = 0;
  private translateOriginY: number = 0;
  private xDist: number = 0;
  private yDist: number = 0;

  constructor(video: HTMLVideoElement, div: HTMLDivElement) {
    this.video = video;
    this.div = div;
  }

  /**
   * mouseWheel: Zoom video around the mouse position by rolling the mouse wheel
   * @param $event The wheel event
   */
  public mouseWheel($event: WheelEvent) {

    // Get the bounding rectangle of target
    const rect = this.div.getBoundingClientRect();
    const deltaY = -$event.deltaY / 400;

    // Keep same transform origin while we are zoomed in to prevent the video jumping if you move the cursor and mouse wheel
    if (this.scale == this.minScale) {
      this.transformOriginX = ($event.clientX - rect.left);
      this.transformOriginY = ($event.clientY - rect.top);
    }

    // Calculate new scale value, keeping it withing the limits minScale - maxScale
    if (deltaY > 0 && this.scale < this.maxScale) {
      this.scale += deltaY;
      this.scale =  Math.min(this.scale, this.maxScale);
    } else if (deltaY < 0 && this.scale > this.minScale) {
      this.scale += deltaY;
      this.scale = Math.max(this.scale, this.minScale);
    }

    this.zoom();

    $event.preventDefault();
    // log("x = "+this.transformOriginX+" y = "+this.transformOriginY);
    // log("Scale = "+this.scale);
  }

  private zoom(transitionDuration: number = 550): void {
    if (this.scale !== this.prevScale) {
      this.video.style.transformOrigin = (this.transformOriginX) + "px " + (this.transformOriginY) + "px";
      this.video.style.transitionProperty = "transform";
      this.video.style.transitionDuration = transitionDuration + "ms";
      this.video.style.transform = "scale(" + this.scale + ")";
      this.prevScale = this.scale;
    }
  }

  private translate(xShift: number, yShift: number) {
//    this.video.style.transform = "translate(" + xShift + "px, " + yShift + "px) scale(" + this.scale + ")";
    console.log(xShift + " : " + yShift);
  }

  reset(slow: boolean = false): void {
    this.scale = this.prevScale = this.minScale;
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = slow ? "200ms" : "0ms";
    this.video.style.transform = "scale(" + this.minScale + ")";
    this.xDist = this.yDist = 0;
  }

  tapTimer: Subscription | null = null;

  touchStartHandler(ev: TouchEvent) {
    const rect = this.div.getBoundingClientRect();
    if (ev.touches.length == 2) {
      if (this.scale === this.minScale) {
        this.transformOriginX = ((ev.touches[0].clientX + ev.touches[1].clientX) / 2 - rect.left) * this.scale;
        this.transformOriginY = ((ev.touches[0].clientY + ev.touches[1].clientY) / 2 - rect.top) * this.scale;
      }
      this.touchStartDist = this.pythagoras(ev);
    } else if (ev.touches.length === 1) { // Handle double tap to reset zoom, or single to shift the zoomed image.
      this.translateOriginX = ev.touches[0].clientX;
      this.translateOriginY = ev.touches[0].clientY;
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
    const rect: DOMRect = this.div.getBoundingClientRect();
    if (ev.touches.length == 2) {
      const dist: number = this.pythagoras(ev);
      const distChange: number = dist - this.touchStartDist;
      this.touchStartDist = dist;
      // Calculate new scale value, keeping it withing the limits minScale - maxScale
      this.scale = this.prevScale + distChange / rect.width * this.fiddleFactor;
      this.scale = Math.min(this.scale, this.maxScale);
      this.scale = Math.max(this.minScale, this.scale);
//      console.log("scale: "+this.scale);
      this.zoom(100);
    } else if (ev.touches.length === 1) {
      const newX: number = ev.touches[0].clientX;
      const newY: number = ev.touches[0].clientY;
      const deltaX: number = newX - this.translateOriginX;
      const deltaY: number = newY - this.translateOriginY
      this.xDist += deltaX;
      this.yDist += deltaY;
      this.translateOriginX = newX; this.translateOriginY = newY;
      this.translate(this.xDist, this.yDist);
    }
    ev.preventDefault();
  }

  touchEndHandler(ev: TouchEvent) {
    this.prevScale = this.scale;
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
