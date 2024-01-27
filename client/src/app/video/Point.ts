/**
 * Point: A class representing a point in the video frame. It provides some arithmetic operations on points
 */
export class Point {
  x: number;
  y: number;

  constructor(x?: number | Point | Event, y?: number | DOMRect) {
    if(typeof(x) === "number" && typeof (y) === "number") {
      this.x = x;
      this.y = y;
    }
    else if(x instanceof Point){
      this.x = x.x;
      this.y = x.y;
    }
    else if (x instanceof MouseEvent || x instanceof WheelEvent) {
      this.x = x.clientX;
      this.y = x.clientY;
      if(y instanceof DOMRect) {
        this.x -= y.left;
        this.y -= y.top;
      }
    }
    else {
      this.x = this.y = 0;
    }
  }

  plus(p: Point | number): Point {
    if(p instanceof Point) {
      this.x += p.x;
      this.y += p.y;
    }
    else {
      this.x += p;
      this.y += p;
    }
    return this;
  }
  minus(p: Point | number): Point {
    if(p instanceof Point) {
      this.y -= p.y;
      this.x -= p.x;
    }
    else {
      this.x -= p;
      this.y -= p;
    }
    return this;
  }
  times(p: Point | number): Point {
    if(p instanceof Point) {
      this.x *= p.x;
      this.y *= p.y;
    }
    else {
      this.x *= p;
      this.y *= p;
    }
    return this;
  }
  dividedBy(p: Point | number): Point {
    if(p instanceof Point) {
      this.x /= p.x;
      this.y /= p.y;
    }
    else {
      this.x /= p;
      this.y /= p;
    }
    return this;
  }

  squared(): Point{
    this.x **= 2;
    this.y **= 2;
    return this;
  }

  fixUpperLimit(upperLimit: Point) {
    if(this.x > upperLimit.x)
      this.x = upperLimit.x;
    if(this.y > upperLimit.y)
      this.y = upperLimit.y;
  }

  fixLowerLimit(lowerLimit: Point) {
    if(this.x < lowerLimit.x)
      this.x = lowerLimit.x;
    if(this.y < lowerLimit.y)
      this.y = lowerLimit.y;
  }
  hypotenuse(p: Point): number {
    const x = this.x;
    const y = this.y;
    this.minus(p);
    this.squared();
    const result = Math.sqrt(this.x + this.y);
    // Restore original values
    this.x = x;
    this.y = y;
    return result;
  }
}
