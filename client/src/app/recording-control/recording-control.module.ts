import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RecordingControlRoutingModule} from "./recording-control-routing.module";
import {RecordingControlComponent} from "./recording-control.component";
import {MatCard, MatCardContent, MatCardModule, MatCardTitle} from "@angular/material/card";
import {MatFormFieldModule, MatHint, MatLabel} from "@angular/material/form-field";
import {MatInput, MatInputModule} from "@angular/material/input";
import {MatTooltip, MatTooltipModule} from "@angular/material/tooltip";
import {MatFormField, MatOption, MatSelect, MatSelectModule} from "@angular/material/select";
import {MatProgressSpinner, MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {SharedModule} from "../shared/shared.module";
import {MatIcon, MatIconModule} from "@angular/material/icon";
import {MatButtonToggle, MatButtonToggleGroup, MatButtonToggleModule} from "@angular/material/button-toggle";
import {MatButton, MatButtonModule} from "@angular/material/button";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {DateAdapter, MAT_DATE_LOCALE, MatNativeDateModule} from "@angular/material/core";
import {
    MatDatepicker,
    MatDatepickerInput,
    MatDatepickerModule,
    MatDatepickerToggle
} from "@angular/material/datepicker";
import {MatDialog, MatDialogModule} from "@angular/material/dialog";
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
        MatDatepicker,
        MatNativeDateModule,
        MatCardTitle,
        MatCardContent,
        MatDatepickerToggle,
        MatDatepickerInput,
        MatOption,
        MatButtonToggleGroup
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
