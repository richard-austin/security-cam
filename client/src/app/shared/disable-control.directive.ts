import {Directive, Input} from '@angular/core';
import {NgControl} from "@angular/forms";
import {timer} from "rxjs";

@Directive({
  selector: '[disableControl]'
})
export class DisableControlDirective {
  @Input() set disableControl(condition: boolean) {
    const action = condition ? 'disable' : 'enable';
    // Deferred execution of the control action otherwise it on sets the initial form state,
    //  and doesn't work for dynamic updates.
    const sub = timer(0).subscribe(() => {
      // @ts-ignore
      this.ngControl.control[action]();
      sub.unsubscribe();
    })
  }

  constructor(private ngControl: NgControl) {
  }
}
