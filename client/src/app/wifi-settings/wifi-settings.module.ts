import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {WifiSettingsComponent} from "./wifi-settings.component";
import {WifiSettingsRoutingModule} from "./wifi-settings-routing.module";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatCardModule} from "@angular/material/card";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {SharedModule} from "../shared/shared.module";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {MatTooltipModule} from "@angular/material/tooltip";
import {MatSelectModule} from "@angular/material/select";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";



@NgModule({
  declarations: [
      WifiSettingsComponent
  ],
  imports: [
      WifiSettingsRoutingModule,
      SharedModule,
      CommonModule,
      MatCardModule,
      MatProgressSpinnerModule,
      MatFormFieldModule,
      MatCheckboxModule,
      MatTooltipModule,
      MatSelectModule,
      FormsModule,
      ReactiveFormsModule
  ]
})
export class WifiSettingsModule { }
