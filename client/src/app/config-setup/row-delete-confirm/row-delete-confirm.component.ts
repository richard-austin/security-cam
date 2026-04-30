import {Component, input, InputSignal, output, OutputEmitterRef} from '@angular/core';
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";
import {UtilsService} from "../../shared/utils.service";

@Component({
  selector: 'app-row-delete-confirm',
  imports: [
    SharedAngularMaterialModule
  ],
  templateUrl: './row-delete-confirm.component.html',
  styleUrl: './row-delete-confirm.component.scss'
})
export class RowDeleteConfirmComponent {
  hideDialogue: OutputEmitterRef<{ cam: string, stream: string }> = output<{ cam: string, stream: string }>();
  delete: OutputEmitterRef<{ cam: string, stream: string }> = output<{ cam: string, stream: string }>();
  camKey: InputSignal<string> = input.required<string>();
  streamKey: InputSignal<string> = input.required<string>();
  name: InputSignal<string> = input<string>("");

  confirmRowDelete(deleteRow: boolean) {
    if (deleteRow)
      this.delete.emit({cam: this.camKey(), stream: this.streamKey()});
    else
      this.hideDialogue.emit({cam: this.camKey(), stream: this.streamKey()});
  }

  protected readonly UtilsService = UtilsService;
}
