import { Directive, ElementRef, Input } from '@angular/core';
import { MatSelect } from '@angular/material/select';
import {timer} from "rxjs";

@Directive({
  selector: '[disableControl]'
})
export class DisableControlDirective {

  constructor(private el: ElementRef<MatSelect>) { }
  @Input() set disable(disabled: boolean)
  {
    timer(1).subscribe(() => {
      if (this.el.nativeElement) {
        this.el.nativeElement.disabled=disabled;
      }
    })
  }
}
