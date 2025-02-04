import { NgModule } from '@angular/core';
import {CommonModule, NgForOf, NgIf} from '@angular/common';
import {MatButton, MatIconButton} from "@angular/material/button";
import {
  MatCell, MatCellDef,
  MatColumnDef,
  MatFooterCell, MatFooterCellDef,
  MatFooterRow, MatFooterRowDef,
  MatHeaderCell, MatHeaderCellDef,
  MatHeaderRow, MatHeaderRowDef,
  MatRow, MatRowDef, MatTable
} from "@angular/material/table";
import {ReactiveFormsModule} from "@angular/forms";
import {MatError, MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatInput} from "@angular/material/input";
import {MatCheckbox} from "@angular/material/checkbox";
import {DisableControlDirective} from "../disable-control.directive";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {MatCard, MatCardContent, MatCardSubtitle, MatCardTitle} from "@angular/material/card";
import {MatProgressSpinner} from "@angular/material/progress-spinner";

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    MatIconButton,
    MatColumnDef,
    MatCell,
    ReactiveFormsModule,
    MatLabel,
    MatFormField,
    MatSelect,
    MatOption,
    MatHeaderCell,
    MatError,
    MatInput,
    MatHint,
    NgIf,
    MatCheckbox,
    MatFooterCell,
    MatHeaderRow,
    MatRow,
    MatFooterRow,
    MatButton,
    NgForOf,
    DisableControlDirective,
    MatCellDef,
    MatHeaderCellDef,
    MatHeaderRowDef,
    MatRowDef,
    MatFooterRowDef,
    MatFooterCellDef,
    MatTooltip,
    MatCard,
    MatCardTitle,
    MatCardSubtitle,
    MatIcon,
    MatCardContent,
    MatProgressSpinner,
    MatTable
  ],
  exports: [
    CommonModule,
    MatIconButton,
    MatColumnDef,
    MatCell,
    ReactiveFormsModule,
    MatLabel,
    MatFormField,
    MatSelect,
    MatOption,
    MatHeaderCell,
    MatError,
    MatInput,
    MatHint,
    NgIf,
    MatCheckbox,
    MatFooterCell,
    MatHeaderRow,
    MatRow,
    MatFooterRow,
    MatButton,
    NgForOf,
    DisableControlDirective,
    MatCellDef,
    MatHeaderCellDef,
    MatHeaderRowDef,
    MatRowDef,
    MatFooterRowDef,
    MatFooterCellDef,
    MatTooltip,
    MatCard,
    MatCardTitle,
    MatCardSubtitle,
    MatIcon,
    MatCardContent,
    MatProgressSpinner,
    MatTable
  ]
})
export class SharedAngularMaterialModule { }
