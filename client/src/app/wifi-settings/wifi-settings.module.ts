import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {WifiSettingsComponent} from "./wifi-settings.component";
import {WifiSettingsRoutingModule} from "./wifi-settings-routing.module";
import {SharedModule} from "../shared/shared.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatCard, MatCardContent, MatCardTitle} from "@angular/material/card";
import {MatCheckbox} from "@angular/material/checkbox";
import {MatTooltip} from "@angular/material/tooltip";
import {MatError, MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatButton} from "@angular/material/button";
import {MatInput} from "@angular/material/input";



@NgModule({
  declarations: [
      WifiSettingsComponent
  ],
    imports: [
        WifiSettingsRoutingModule,
        SharedModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        MatCard,
        MatCardTitle,
        MatCardContent,
        MatCheckbox,
        MatTooltip,
        MatLabel,
        MatHint,
        MatError,
        MatFormField,
        MatProgressSpinner,
        MatSelect,
        MatOption,
        MatButton,
        MatInput
    ]
})
export class WifiSettingsModule { }
