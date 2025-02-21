import { NgModule } from '@angular/core';
import {CommonModule, NgForOf, NgIf} from '@angular/common';
import {MatButtonModule, MatIconButton} from "@angular/material/button";
import {MatTableModule} from "@angular/material/table";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatError, MatFormField, MatLabel} from "@angular/material/form-field";
import {MatSelectModule} from "@angular/material/select";
import {MatInputModule} from "@angular/material/input";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {DisableControlDirective} from "../disable-control.directive";
import {MatTooltipModule} from "@angular/material/tooltip";
import {MatIconModule} from "@angular/material/icon";
import {MatCardContent, MatCardModule} from "@angular/material/card";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MatRadioButton, MatRadioGroup, MatRadioModule} from "@angular/material/radio";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatButtonToggle, MatButtonToggleGroup, MatButtonToggleModule} from "@angular/material/button-toggle";
import {MatNativeDateModule} from "@angular/material/core";

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    DisableControlDirective,
    MatRadioModule,
    MatButtonModule,
    MatRadioModule,
    MatButtonToggleModule,
    MatInputModule,
    MatCardModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCheckboxModule
  ],
  exports: [
    CommonModule,
    MatIconButton,
    ReactiveFormsModule,
    MatLabel,
    MatFormField,
    MatSelectModule,
    MatError,
    MatInputModule,
    NgIf,
    MatCheckboxModule,
    MatButtonModule,
    NgForOf,
    DisableControlDirective,
    MatTooltipModule,
    MatCardModule,
    MatIconModule,
    MatCardContent,
    MatProgressSpinnerModule,
    MatTableModule,
    MatRadioGroup,
    MatRadioButton,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonToggleGroup,
    FormsModule,
    MatButtonToggle
  ],
  providers:[MatDatepickerModule]
})
export class SharedAngularMaterialModule { }
