import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ConfigSetupComponent} from "./config-setup.component";
import {AddAsOnvifDeviceComponent} from "./add-as-onvif-device/add-as-onvif-device.component";
import {OnvifCredentialsComponent} from "./camera-credentials/onvif-credentials.component";
import {MatCard, MatCardContent, MatCardSubtitle, MatCardTitle} from "@angular/material/card";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {SharedModule} from "../shared/shared.module";
import {MatButton, MatIconButton} from "@angular/material/button";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {OnvifFailuresComponent} from "./onvif-failures/onvif-failures.component";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatFooterCell, MatFooterCellDef, MatFooterRow, MatFooterRowDef,
  MatHeaderCell,
  MatHeaderCellDef, MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef,
  MatTable
} from "@angular/material/table";
import {MatError, MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {MatOption, MatSelect} from "@angular/material/select";
import {DisableControlDirective} from "../shared/disable-control.directive";
import {MatInput} from "@angular/material/input";
import {ReactiveFormsModule} from "@angular/forms";
import {MatCheckbox} from "@angular/material/checkbox";
import {ExcludeOwnStreamPipe} from "./exclude-own-stream.pipe";
import {ConfigSetupRoutingModule} from "./config-setup-routing.module";

@NgModule({
  declarations: [
      ConfigSetupComponent,
      AddAsOnvifDeviceComponent,
      OnvifCredentialsComponent,
      OnvifFailuresComponent,
      DisableControlDirective,
      ExcludeOwnStreamPipe,
  ],
  imports: [
    ConfigSetupRoutingModule,
    CommonModule,
    MatCard,
    MatCardTitle,
    MatCardSubtitle,
    MatTooltip,
    MatIcon,
    SharedModule,
    MatIconButton,
    MatCardContent,
    MatProgressSpinner,
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatCellDef,
    MatFormField,
    MatSelect,
    MatOption,
    MatCell,
    MatInput,
    ReactiveFormsModule,
    MatHint,
    MatError,
    MatLabel,
    MatCheckbox,
    MatFooterCell,
    MatHeaderRowDef,
    MatRowDef,
    MatFooterRowDef,
    MatFooterCellDef,
    MatHeaderRow,
    MatRow,
    MatFooterRow,
    MatButton
  ]
})
export class ConfigSetupModule { }
