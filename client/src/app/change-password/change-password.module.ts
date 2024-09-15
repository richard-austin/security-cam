import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ChangePasswordRoutingModule } from './change-password-routing.module';
import { ChangePasswordComponent } from './change-password.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButton} from "@angular/material/button";
import {MatCard, MatCardContent, MatCardTitle} from "@angular/material/card";
import {MatError, MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {MatInput} from "@angular/material/input";
import {SharedModule} from "../shared/shared.module";


@NgModule({
  declarations: [
    ChangePasswordComponent,
  ],
    imports: [
        CommonModule,
        ChangePasswordRoutingModule,
        FormsModule,
        MatButton,
        MatCard,
        MatCardContent,
        MatCardTitle,
        MatError,
        MatFormField,
        MatHint,
        MatInput,
        MatLabel,
        ReactiveFormsModule,
        SharedModule,
    ]
})
export class ChangePasswordModule { }
