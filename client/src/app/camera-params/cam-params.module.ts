import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CamParamsRoutingModule} from "./cam-params-routing.module";
import {MatCardModule} from "@angular/material/card";
import {CameraParamsComponent} from "./camera-params.component";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatSelectModule} from "@angular/material/select";
import {MatTooltipModule} from "@angular/material/tooltip";
import {SharedModule} from "../shared/shared.module";
import {MatButtonModule} from "@angular/material/button";
import {MatInputModule} from "@angular/material/input";

@NgModule({
    declarations: [
        CameraParamsComponent
    ],
  imports: [
    CamParamsRoutingModule,
    SharedModule,
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatInputModule,
    MatTooltipModule
  ]
})
export class CamParamsModule {
}
