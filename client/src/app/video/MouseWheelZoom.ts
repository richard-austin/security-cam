export class MouseWheelZoom {
  transformOriginX: number = 0;
  transformOriginY: number = 0;
  readonly maxScale: number = 6;
  readonly minScale: number = 1;
  readonly video: HTMLVideoElement;
  readonly div: HTMLDivElement;
  scale: number = this.minScale;
  prevScale: number = this.minScale;
  touchStartDist!: number;  // Distance aprt of fingers at the touchStart
  readonly fiddleFactor: number = 5;  // Multiplier to get the best pinch zoom response

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
      this.scale = this.scale > this.maxScale ? this.maxScale : this.scale;
    } else if (deltaY < 0 && this.scale > this.minScale) {
      this.scale += deltaY;
      this.scale = this.scale < this.minScale ? this.minScale : this.scale;
    }

    this.zoom();

    $event.preventDefault();
    // log("x = "+this.transformOriginX+" y = "+this.transformOriginY);
    // log("Scale = "+this.scale);
  }

  reset(slow: boolean = false): void {
    this.scale = this.prevScale = this.minScale;
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = slow ? "200ms" : "0ms";
    this.video.style.transform = "scale(" + this.minScale + ")";
  }


  touchStartHandler(ev: TouchEvent) {
    if (ev.touches.length == 2) {
      const rect = this.div.getBoundingClientRect();
      if (this.scale === this.minScale) {
        this.transformOriginX = (ev.touches[0].clientX + ev.touches[1].clientX) / 2 -rect.left;
        this.transformOriginY = (ev.touches[0].clientY + ev.touches[1].clientY) / 2 - rect.top;
      }
      this.touchStartDist = Math.sqrt((ev.touches[0].clientX - ev.touches[1].clientX) ** 2 +
        (ev.touches[0].clientY - ev.touches[1].clientY) ** 2);
    }
    console.log("transformOriginX: "+this.transformOriginX+" transformOriginY: "+this.transformOriginY);
  }

  touchMoveHandler(ev: TouchEvent) {
    if (ev.touches.length == 2) {
      const rect = this.div.getBoundingClientRect();

      let dist: number = Math.sqrt((ev.touches[0].clientX - ev.touches[1].clientX) ** 2 +
        (ev.touches[0].clientY - ev.touches[1].clientY) ** 2);
      let distChange: number = dist - this.touchStartDist ;
      this.touchStartDist = dist;
      // Calculate new scale value, keeping it withing the limits minScale - maxScale
      this.scale = this.prevScale + distChange / rect.width * this.fiddleFactor;
      if(this.scale > this.maxScale)
        this.scale = this.maxScale;
      else if(this.scale < this.minScale)
        this.scale = this.minScale;

      console.log("scale: "+this.scale);
      this.zoom(100);
    }
    ev.preventDefault();
  }
  private zoom(transitionDuration: number=550): void {
    if (this.scale !== this.prevScale) {
      this.video.style.transformOrigin = this.transformOriginX + "px " + this.transformOriginY + "px";
      this.video.style.transitionProperty = "transform";
      this.video.style.transitionDuration = transitionDuration+"ms";
      this.video.style.transform = "scale(" + this.scale + ")";
      this.prevScale = this.scale;
    }
  }

  touchEndHandler(ev: TouchEvent) {
    this.prevScale = this.scale;
    // console.log("TouchEnd: " + ev.touches.length);
    // for (let i = 0; i < ev.touches.length; i++) {
    //   let touch: Touch = ev.touches[i];
    //   console.log("Touch: " + (i + 1) + " clientX: " + touch.clientX + " clientY: " + touch.clientY)
    // }
    ev.preventDefault()
  }
}
