import {Directive, HostListener, Input} from '@angular/core';
import {ConfigSetupComponent} from "./config-setup.component";

@Directive({
  selector: '[appEditableOnBlur]'
})
export class EditableOnBlurDirective {
  @Input('camIndex') camIndex!: number;
  @Input('streamIndex') streamIndex!: number;
  @Input('field') field!: string;

  constructor(private csc: ConfigSetupComponent) { }

  @HostListener('blur')
  onBlur()
  {
    if(this.streamIndex !== undefined)
      this.csc.updateStreamField(this.camIndex, this.streamIndex, this.field);
    else
      this.csc.updateCamField(this.camIndex, this.field);
  }
}
