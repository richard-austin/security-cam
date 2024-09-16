import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RecordingControlRoutingModule} from "./recording-control-routing.module";
import {RecordingControlComponent} from "./recording-control.component";
import {MatCardModule} from "@angular/material/card";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {MatTooltipModule} from "@angular/material/tooltip";
import {MatSelectModule} from "@angular/material/select";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {SharedModule} from "../shared/shared.module";
import {MatIconModule} from "@angular/material/icon";
import {MatButtonToggleModule} from "@angular/material/button-toggle";
import {MatButtonModule} from "@angular/material/button";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {DateAdapter, MAT_DATE_LOCALE, MatNativeDateModule} from "@angular/material/core";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatDialogModule} from "@angular/material/dialog";
import {CustomDateAdapter} from "../cameras/camera.service";
import {Platform} from "@angular/cdk/platform";


@NgModule({
    declarations: [
        RecordingControlComponent
    ],
    imports: [
        RecordingControlRoutingModule,
        SharedModule,
        CommonModule,
        MatCardModule,
        MatInputModule,
        MatProgressSpinnerModule,
        MatIconModule,
        MatButtonToggleModule,
        MatButtonModule,
        MatSelectModule,
        MatDialogModule,
        FormsModule,
        ReactiveFormsModule,
        MatTooltipModule,
        MatFormFieldModule,
        MatDatepickerModule,
        MatNativeDateModule
    ],
    providers: [
        {
            provide: DateAdapter,
            useClass: CustomDateAdapter,
            deps: [MAT_DATE_LOCALE, Platform]
        }
    ]
})
export class RecordingControlModule {
}
