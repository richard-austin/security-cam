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
import {GetLocalWifiDetailsComponent} from "../get-local-wifi-details/get-local-wifi-details.component";
import {MatIcon} from "@angular/material/icon";
import {
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell, MatHeaderCellDef,
    MatHeaderRow, MatHeaderRowDef,
    MatRow, MatRowDef,
    MatTable
} from "@angular/material/table";



@NgModule({
  declarations: [
      WifiSettingsComponent,
      GetLocalWifiDetailsComponent
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
        MatInput,
        MatIcon,
        MatCell,
        MatHeaderCell,
        MatColumnDef,
        MatTable,
        MatCellDef,
        MatRow,
        MatHeaderRow,
        MatRowDef,
        MatHeaderCellDef,
        MatHeaderRowDef
    ]
})
export class WifiSettingsModule { }
