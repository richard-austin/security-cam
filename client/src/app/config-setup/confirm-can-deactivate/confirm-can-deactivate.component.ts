import {Component, output, OutputEmitterRef} from '@angular/core';
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";

@Component({
  selector: 'app-confirm-can-deactivate',
  imports: [
      SharedAngularMaterialModule
  ],
  templateUrl: './confirm-can-deactivate.component.html',
  styleUrl: './confirm-can-deactivate.component.scss'
})
export class ConfirmCanDeactivateComponent {
  signalConfirmExit: OutputEmitterRef<boolean> = output<boolean>();

  confirmExit(exit: boolean) {
    this.signalConfirmExit.emit(exit);
  }
}
