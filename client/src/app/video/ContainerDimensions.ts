export class ContainerDimensions {
  readonly top: number;
  readonly left: number;
  readonly width: number;
  readonly height: number;
  constructor(div: HTMLDivElement) {
    const rect = div.getBoundingClientRect();
    const style = window.getComputedStyle(div);
    const borderLeft = style.borderTopWidth !== "" ? parseInt(style.borderTopWidth) : 0;
    const borderRight = style.borderRightWidth !== "" ? parseInt(style.borderRightWidth) : 0;
    const borderTop = style.borderTopWidth !== "" ? parseInt(style.borderTopWidth) : 0;
    const borderBottom: number = style.borderBottomWidth !== "" ? parseInt(style.borderBottomWidth) : 0;
    const paddingLeft = style.paddingTop !== "" ? parseInt(style.paddingTop) : 0;
    const paddingRight = style.paddingRight !== "" ? parseInt(style.paddingRight) : 0;
    const paddingTop = style.paddingTop !== "" ? parseInt(style.paddingTop) : 0;
    const paddingBottom: number = style.paddingBottom !== "" ? parseInt(style.paddingBottom) : 0;
    this.top = rect.top+borderTop+paddingTop;
    this.left = rect.left+borderLeft+paddingLeft;
    this.width = rect.width-borderLeft-borderRight-paddingRight;
    this.height = rect.height-borderTop-borderBottom-paddingBottom;
  }
}
