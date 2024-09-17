import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {SetUpGuestAccountComponent} from "./set-up-guest-account.component";
import {SetUpGuestAccountRoutingModule} from "./set-up-guest-account-routing.module";
import {SharedModule} from "../shared/shared.module";
import {MatCard, MatCardContent, MatCardTitle} from "@angular/material/card";
import {MatCheckbox} from "@angular/material/checkbox";
import {ReactiveFormsModule} from "@angular/forms";
import {MatTooltip} from "@angular/material/tooltip";
import {MatError, MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {MatInput} from "@angular/material/input";
import {MatButton} from "@angular/material/button";

@NgModule({
  declarations: [
      SetUpGuestAccountComponent
  ],
  imports: [
    CommonModule,
    SetUpGuestAccountRoutingModule,
    SharedModule,
    MatCard,
    MatCardTitle,
    MatCardContent,
    MatCheckbox,
    ReactiveFormsModule,
    MatTooltip,
    MatFormField,
    MatLabel,
    MatHint,
    MatError,
    MatInput,
    MatButton,
  ]
})
export class SetUpGuestAccountModule { }
