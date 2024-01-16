export class MouseWheelZoom {
  transformOriginX: number = 0;
  transformOriginY: number = 0;
  readonly maxScale: number = 4;
  readonly minScale: number = 1;
  readonly video: HTMLVideoElement;
  readonly div: HTMLDivElement;
  scale: number = this.minScale;
  prevScale: number = this.minScale;

  constructor(video: HTMLVideoElement, div:HTMLDivElement) {
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
    const deltaY = -$event.deltaY / 1000;

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

    if(this.scale !== this.prevScale) {
      this.video.style.transformOrigin = this.transformOriginX+"px "+this.transformOriginY+"px";
      this.video.style.transitionProperty = "transform";
      this.video.style.transitionDuration = "550ms";
      this.video.style.transform = "scale("+this.scale+")";
      this.prevScale = this.scale;
    }

    $event.preventDefault();
    // console.log("x = "+this.transformOriginX+" y = "+this.transformOriginY);
    // console.log("Scale = "+this.scale);
  }
  reset(slow: boolean = false): void {
    this.scale = this.prevScale = this.minScale;
    this.video.style.transitionProperty = "transform";
    this.video.style.transitionDuration = slow ? "200ms" : "0ms";
    this.video.style.transform = "scale("+this.minScale+")";
  }
}
