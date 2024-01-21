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
    else {
      this.xDist = this.yDist = 0;  // Reset any xy pan if continuing zoom
    }

    // Calculate new scale value, keeping it withing the limits minScale - maxScale
    if (deltaY > 0 && this.scale < this.maxScale) {
      this.scale += deltaY;
      this.scale =  Math.min(this.scale, this.maxScale);
    } else if (deltaY < 0 && this.scale > this.minScale) {
      this.scale += deltaY;
      this.scale = Math.max(this.scale, this.minScale);
    }

    this.transform(this.xDist, this.yDist);

    $event.preventDefault();
    // log("x = "+this.transformOriginX+" y = "+this.transformOriginY);
    // log("Scale = "+this.scale);
  }

  bMouseDown: boolean = false;
  bDragging: boolean = false;
  mouseDown(ev: MouseEvent) {
    this.bMouseDown = true;
    this.translateOriginX = ev.clientX;
    this.translateOriginY = ev.clientY;

    console.log("mouseDown: clientX = "+ev.clientX+" clientY = "+ev.clientY);
  }
  mouseMove(ev: MouseEvent) {
    if(this.bMouseDown) {
      this.bDragging = true;
      const newX: number = ev.clientX;
      const newY: number = ev.clientY;
      this.transform(newX, newY);
      console.log("mouseMove: clientX = "+ev.clientX+" clientY = "+ev.clientY);
    }
  }

  mouseUp(ev: MouseEvent) {
    if(this.bDragging)
      this.prevScale = this.scale;
    this.bMouseDown = this.bDragging = false;
    console.log("mouseMove: clientX = "+ev.clientX+" clientY = "+ev.clientY);
  }

  private zoom(transitionDuration: number = 550): void {
    if (this.scale !== this.prevScale) {
      this.video.style.transformOrigin = (this.transformOriginX) + "px " + (this.transformOriginY) + "px";
      this.video.style.transitionProperty = "transform";
      this.video.style.transitionDuration = transitionDuration + "ms";
      this.video.style.transform = "translate(" + this.xDist + "px, " + this.yDist + "px) scale(" + this.scale + ")";
 //     this.video.style.transform = "scale(" + this.scale + ") translate(" + this.xDist + "px, " + this.yDist + "px";
      this.prevScale = this.scale;
    }
  }

  private transform(newX: number, newY: number) {
    const deltaX: number = newX - this.translateOriginX;
    const deltaY: number = newY - this.translateOriginY
    this.xDist += deltaX;
    this.yDist += deltaY;
    this.translateOriginX = newX; this.translateOriginY = newY;
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = 550 + "ms";
    this.video.style.transformOrigin = (this.transformOriginX) + "px " + (this.transformOriginY) + "px";
    this.fixWithinViewPort();
    this.video.style.transform = "translate(" + this.xDist + "px, " + this.yDist + "px) scale(" + this.scale + ")";
//    console.log(this.xDist + " : " + this.yDist);
  }

  private fixWithinViewPort() {
    const rect: DOMRect =  this.div.getBoundingClientRect();
    const width = rect.width-this.div.clientLeft;
    const height = rect.height-this.div.clientTop;
    if(this.xDist + this.transformOriginX * (1-this.scale) > 0)
      this.xDist = -this.transformOriginX * (1-this.scale)
    else if(this.xDist + this.transformOriginX + (width -this.transformOriginX) * this.scale <width)
      this.xDist = -(width -this.transformOriginX) * this.scale -this.transformOriginX + width;

    if(this.yDist + this.transformOriginY * (1-this.scale) > 0)
      this.yDist = -this.transformOriginY * (1-this.scale);
    else if(this.yDist + this.transformOriginY + (height -this.transformOriginY) * this.scale <height)
      this.yDist = -(height -this.transformOriginY) * this.scale -this.transformOriginY + height;
  }

  reset(slow: boolean = false): void {
    this.scale = this.prevScale = this.minScale;
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = slow ? "200ms" : "0ms";
    this.video.style.transform = "scale(" + this.minScale + ")";
    this.xDist = this.yDist = 0;
  }

  private tapTimer: Subscription | null = null;
  private currentTouches: number = 0;
  touchStartHandler(ev: TouchEvent) {
    console.log("touchStartHandler: touches="+ev.touches.length);
    this.currentTouches = ev.touches.length;
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
    console.log("touchMoveHandler: touches="+ev.touches.length);
    const rect: DOMRect = this.div.getBoundingClientRect();
    if (ev.touches.length == 2 && this.currentTouches === ev.touches.length) {
      const dist: number = this.pythagoras(ev);
      const distChange: number = dist - this.touchStartDist;
      this.touchStartDist = dist;
      // Calculate new scale value, keeping it withing the limits minScale - maxScale
      this.scale = this.prevScale + distChange / rect.width * this.fiddleFactor;
      this.scale = Math.min(this.scale, this.maxScale);
      this.scale = Math.max(this.minScale, this.scale);
//      console.log("scale: "+this.scale);
      this.zoom(100);
    } else if (ev.touches.length === 1 && this.currentTouches === ev.touches.length) {
      const newX: number = ev.touches[0].clientX;
      const newY: number = ev.touches[0].clientY;
      this.transform(newX, newY);
    }
    if(this.scale !== this.minScale)
      ev.preventDefault();  // Allow touchMove default action if no zoom to allow scrolling of multicam page on smartphone
  }

  touchEndHandler(ev: TouchEvent) {
    console.log("touchEndHandler: touches="+ev.touches.length);
    this.currentTouches = ev.touches.length == 0 ? 0 : this.currentTouches;
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
