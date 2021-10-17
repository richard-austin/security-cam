import {Directive, HostListener, Input} from '@angular/core';
import {ConfigSetupComponent} from "./config-setup.component";

@Directive({
  selector: '[appEditableOnBlur]'
})
export class EditableOnBlurDirective {
  @Input('index') index!: number;
  @Input('field') field!: string;

  constructor(private csc: ConfigSetupComponent) { }

  @HostListener('blur')
  onBlur()
  {
    this.csc.updateField(this.index, this.field);
  }
}
