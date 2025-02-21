import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {GeneralRoutingModule} from "./general-routing.module";
import {SharedModule} from "../shared/shared.module";
import {MatCardModule} from "@angular/material/card";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {ReactiveFormsModule} from "@angular/forms";
import {MatTooltipModule} from "@angular/material/tooltip";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {SetIpComponent} from "../set-ip/set-ip.component";
import {SetUpGuestAccountComponent} from "../set-up-guest-account/set-up-guest-account.component";
import {AboutComponent} from "../about/about.component";
import {CloudProxyComponent} from "../cloud-proxy/cloud-proxy.component";
import {DrawdownCalcContainerComponent} from "../drawdown-calc-container/drawdown-calc-container.component";
import {MatTableModule
} from "@angular/material/table";
import {
  CreateUserAccountContainerComponent
} from "../create-user-account-container/create-user-account-container.component";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MatDatepickerModule, MatDatepickerToggle, MatDatepickerToggleIcon} from "@angular/material/datepicker";

@NgModule({
  declarations: [
      SetUpGuestAccountComponent,
      SetIpComponent,
      DrawdownCalcContainerComponent,
      CreateUserAccountContainerComponent,
      CloudProxyComponent,
      AboutComponent
  ],
  imports: [
    CommonModule,
    GeneralRoutingModule,
    SharedModule,
    MatCardModule,
    MatInputModule,
    MatDatepickerModule,
    MatDatepickerToggle,
    MatDatepickerToggleIcon,
    MatFormFieldModule,
    MatCheckboxModule,
    ReactiveFormsModule,
    MatTooltipModule,
    MatButtonModule,
    MatTableModule,
    MatProgressSpinnerModule,
  ]
})
export class GeneralModule { }
