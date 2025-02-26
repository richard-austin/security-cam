import {Component, EventEmitter, Input, Output} from '@angular/core';
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";

@Component({
  selector: 'app-row-delete-confirm',
    imports: [
        SharedAngularMaterialModule
    ],
  templateUrl: './row-delete-confirm.component.html',
  styleUrl: './row-delete-confirm.component.scss'
})
export class RowDeleteConfirmComponent {
    @Output() hideDialogue: EventEmitter<{cam: string, stream: string}> = new EventEmitter<{cam: string, stream: string}>();
    @Input() camKey!: string;
    @Input() streamKey!: string;

    confirmRowDelete(deleteRow: boolean) {
        if(deleteRow)
            this.hideDialogue.emit({cam:this.camKey, stream: this.streamKey});
        else
            this.hideDialogue.emit({cam:'', stream:''});
    }
}
