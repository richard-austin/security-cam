import {Subscription, timer} from "rxjs";
import {Point} from "./Point";

export class VideoTransformations {
  private readonly maxScale: number = 6;
  private readonly minScale: number = 1;
  private readonly video: HTMLVideoElement;
  private readonly div: HTMLDivElement;
  private scale: number = this.minScale;

  private touchStartDist!: number;  // Distance aprt of fingers at the touchStart
  readonly fiddleFactor: number = 5;  // Multiplier to get the best pinch zoom response
  private readonly offset: Point;
  private panStart!: Point;

  bMouseDown: boolean = false;
  bDragging: boolean = false;

  constructor(video: HTMLVideoElement, div: HTMLDivElement) {
    this.offset = new Point();
    this.video = video;
    this.div = div;
  }

  /**
   * mouseWheel: Zoom video around the mouse position by rolling the mouse wheel
   * @param ev The wheel event
   */
  public mouseWheel(ev: WheelEvent) {
    this.zoom(ev);
  }

  mouseDown(ev: MouseEvent) {
    this.bMouseDown = true;
    this.panStart = new Point(ev);
  }

  mouseMove(ev: MouseEvent) {
    if (this.bMouseDown) {
      const panDelta = new Point(ev).minus(this.panStart);
      this.offset.plus(panDelta);
      this.fixWithinViewPort();
      this.transform(true);
      this.panStart = new Point(ev);
    }
  }
  mouseUp(ignore: MouseEvent) {
    this.bMouseDown = false;
    if (this.bDragging) {
      this.bDragging = false;
    }
  }

  private zoom(ev: Event) {
    const rect = this.div.getBoundingClientRect();
    const sc0 = this.scale;  // Save initial scale
    if (ev instanceof WheelEvent) {
      // Get the bounding rectangle of target
      const deltaWheel = -ev.deltaY * this.scale / 400;
      this.scale += deltaWheel;  // Adjust the scale
      // Ensure scale limits
      this.setScaleWithinLimits();
      // Adjust the offset to keep the focus point at the same screen position
      this.offset.minus(this.deltaOffset(sc0, this.scale, new Point(ev, rect)));
      this.fixWithinViewPort();  // Ensure video borders don't end up inside the viewport borders
      this.transform();
    }
    else if(ev instanceof TouchEvent) {
      const dist: number = this.hypotenuse(ev);
      const distChange: number = dist - this.touchStartDist;
      this.touchStartDist = dist;
      const prevScale = this.scale;
      const x1 = Math.abs(ev.touches[0].clientX + ev.touches[1].clientX) / 2;
      const y1 = Math.abs(ev.touches[0].clientY + ev.touches[1].clientY) / 2;

      this.scale += distChange / rect.width * this.fiddleFactor;
      // Ensure scale limits
      this.setScaleWithinLimits();
      this.offset.minus(this.deltaOffset(sc0, this.scale, new Point(x1, y1).minus(new Point(rect.left, rect.top))));
      this.fixWithinViewPort();  // Ensure video borders don't end up inside the viewport borders
      this.transform(true);
    }
    ev.preventDefault();
  }

  deltaOffset(sc0: number, sc1: number, mousePos: Point): Point {
    const origTotalLength: Point = new Point(this.offset).times(-1).plus(mousePos);
    const newTotalLength: Point = new Point(origTotalLength).times(sc1 / sc0);
    return new Point(newTotalLength).minus(origTotalLength);
  }

  private transform(fast: boolean = false) {
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = (fast ? 0 : 550) + "ms";
    this.video.style.transformOrigin = 0 + " " + 0;
    this.video.style.transform = "translate(" + this.offset.x + "px, " + this.offset.y + "px) scale(" + this.scale + ")";
  }

  private fixWithinViewPort() {
    const rect: DOMRect = this.div.getBoundingClientRect();
    const width = rect.width - this.div.clientLeft;
    const height = rect.height - this.div.clientTop;
    this.offset.fixUpperLimit(new Point());
    const lowerLimit = new Point(width, height).times(1 - this.scale);
    this.offset.fixLowerLimit(lowerLimit);
  }
  private setScaleWithinLimits() {
    this.scale = Math.min(this.scale, this.maxScale);
    this.scale = Math.max(this.scale, this.minScale);
  }
  reset(slow: boolean = false): void {
    this.scale = this.minScale;
    this.offset.x = this.offset.y = 0;
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = slow ? "200ms" : "0ms";
    this.video.style.transform = "scale(" + this.minScale + ")";
  }

  private tapTimer: Subscription | null = null;
  private currentTouches: number = 0;

  touchStartHandler(ev: TouchEvent) {
//    console.log("touchStartHandler: touches=" + ev.touches.length);
    this.currentTouches = ev.touches.length;
    const rect = this.div.getBoundingClientRect();
    if (ev.touches.length == 2) {
      if (this.scale === this.minScale) {
        this.reset();
      }
      this.touchStartDist = this.hypotenuse(ev);
    } else if (ev.touches.length === 1) { // Handle double tap to reset zoom, or single to shift the zoomed image.
      this.panStart = new Point(ev.touches[0].clientX, ev.touches[0].clientY);
      if (this.tapTimer == null) {  // 1 tap
        this.tapTimer = timer(300).subscribe(() => {
          // @ts-ignore
          this.tapTimer.unsubscribe();
          this.tapTimer = null;
        });
      } else {  // 2 taps
        this.tapTimer.unsubscribe();
        this.tapTimer = null;
        this.reset(true);
      }
    }
  }
  touchMoveHandler(ev: TouchEvent) {
    // console.log("touchMoveHandler: touches=" + ev.touches.length);
    const rect: DOMRect = this.div.getBoundingClientRect();
    if (ev.touches.length == 2 && this.currentTouches === ev.touches.length) {
      this.zoom(ev);
    } else if (ev.touches.length === 1 && this.currentTouches === ev.touches.length) {
      const panNow = new Point(new Point(ev.touches[0].clientX, ev.touches[0].clientY));
      const panDelta = new Point(panNow).minus(this.panStart);
      this.offset.plus(panDelta);
      this.panStart = panNow;
      this.fixWithinViewPort();
      this.transform(true);
    }
    if (this.scale !== this.minScale)
      ev.preventDefault();  // Allow touchMove default action if no zoom to allow scrolling of multicam page on smartphone
  }

   touchEndHandler(ev: TouchEvent) {
    this.currentTouches = ev.touches.length == 0 ? 0 : this.currentTouches;
    ev.preventDefault()
  }
  hypotenuse(ev: TouchEvent): number {
    if (ev.touches.length === 2)
      return Math.sqrt((ev.touches[0].clientX - ev.touches[1].clientX) ** 2 +
        (ev.touches[0].clientY - ev.touches[1].clientY) ** 2);
    else
      return 0;
  }
}
