import {Directive, effect, EffectRef, input, InputSignal, OnDestroy} from '@angular/core';
import {NgControl} from "@angular/forms";
import {timer} from "rxjs";

@Directive({
  selector: '[disableControl]'
})
export class DisableControlDirective implements OnDestroy{
  disableControl: InputSignal<boolean> = input.required<boolean>();

  effectRef:EffectRef;
  constructor(private ngControl: NgControl) {
    this.effectRef = effect(() => {
      const condition = this.disableControl();
      const action = condition ? 'disable' : 'enable';
      // Deferred execution of the control action otherwise it sets the initial form state,
      //  and doesn't work for dynamic updates.
      const sub = timer(0).subscribe(() => {
        this.ngControl.control?.[action]();
        sub.unsubscribe();
      })
    });
  }

  ngOnDestroy(): void {
      this.effectRef.destroy();
    }
  }
