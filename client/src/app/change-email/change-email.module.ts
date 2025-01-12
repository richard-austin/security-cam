import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ChangeEmailComponent} from "./change-email.component";
import {MatCard, MatCardContent, MatCardTitle} from "@angular/material/card";
import {MatError, MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {SharedModule} from "../shared/shared.module";
import {MatButton} from "@angular/material/button";
import {MatInput} from "@angular/material/input";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ChangeEmailRoutingModule} from "./change-email-routing.module";
import {MatTooltip} from "@angular/material/tooltip";

@NgModule({
  declarations: [
      ChangeEmailComponent
  ],
    imports: [
        ChangeEmailRoutingModule,
        CommonModule,
        SharedModule,
        FormsModule,
        ReactiveFormsModule,
        MatCard,
        MatCardTitle,
        MatLabel,
        MatHint,
        MatError,
        MatCardContent,
        MatFormField,
        MatButton,
        MatInput,
        MatTooltip
    ]
})
export class ChangeEmailModule { }
