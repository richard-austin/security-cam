import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RecordingControlRoutingModule} from "./recording-control-routing.module";
import {RecordingControlComponent} from "./recording-control.component";
import {MatCard, MatCardContent, MatCardTitle} from "@angular/material/card";
import {MatHint, MatLabel, MatSuffix} from "@angular/material/form-field";
import {MatInput} from "@angular/material/input";
import {MatTooltip} from "@angular/material/tooltip";
import {MatFormField, MatOption, MatSelect} from "@angular/material/select";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {SharedModule} from "../shared/shared.module";
import {MatIcon} from "@angular/material/icon";
import {MatButtonToggle, MatButtonToggleGroup} from "@angular/material/button-toggle";
import {MatButton} from "@angular/material/button";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {DateAdapter, MAT_DATE_LOCALE, MatNativeDateModule} from "@angular/material/core";
import {CustomDateAdapter} from "../cameras/camera.service";
import {Platform} from "@angular/cdk/platform";
import {
    MatDatepickerInput,
    MatDatepickerModule,
    MatDatepickerToggle,
} from "@angular/material/datepicker";


@NgModule({
    declarations: [
        RecordingControlComponent
    ],
    imports: [
        RecordingControlRoutingModule,
        SharedModule,
        CommonModule,
        MatCard,
        MatInput,
        MatProgressSpinner,
        MatIcon,
        MatButtonToggle,
        MatButton,
        MatSelect,
        FormsModule,
        ReactiveFormsModule,
        MatTooltip,
        MatFormField,
        MatLabel,
        MatHint,
        MatCardTitle,
        MatCardContent,
        MatOption,
        MatButtonToggleGroup,
        MatNativeDateModule,
        MatDatepickerModule,
        MatDatepickerToggle,
        MatDatepickerInput,
        MatSuffix
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
