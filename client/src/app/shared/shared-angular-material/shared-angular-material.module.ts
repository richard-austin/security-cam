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
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatError, MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatInput} from "@angular/material/input";
import {MatCheckbox} from "@angular/material/checkbox";
import {DisableControlDirective} from "../disable-control.directive";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {MatCard, MatCardContent, MatCardSubtitle, MatCardTitle} from "@angular/material/card";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {MatRadioButton, MatRadioGroup} from "@angular/material/radio";
import {
  MatDatepicker,
  MatDatepickerInput,
  MatDatepickerModule,
  MatDatepickerToggle
} from "@angular/material/datepicker";
import {MatButtonToggle, MatButtonToggleGroup} from "@angular/material/button-toggle";
import {MatNativeDateModule} from "@angular/material/core";

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
    MatTable,
    MatRadioGroup,
    MatRadioButton,
    MatDatepickerInput,
    MatDatepickerToggle,
    MatDatepicker,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonToggleGroup,
    FormsModule,
    MatButtonToggle
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
    MatTable,
    MatRadioGroup,
    MatRadioButton,
    MatDatepickerInput,
    MatDatepickerToggle,
    MatDatepicker,
    MatButtonToggleGroup,
    FormsModule,
    MatButtonToggle
  ],
  providers:[MatDatepickerModule]
})
export class SharedAngularMaterialModule { }
