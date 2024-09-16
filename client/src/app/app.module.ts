import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CamerasComponent } from './cameras/cameras.component';
import { HttpClient, provideHttpClient, withInterceptorsFromDi } from "@angular/common/http";
import {BaseUrl} from "./shared/BaseUrl/BaseUrl";
import { NavComponent } from './nav/nav.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatCheckboxModule} from "@angular/material/checkbox";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatButtonToggleModule} from "@angular/material/button-toggle";
import {MatCardModule} from "@angular/material/card";
import { RecordingControlComponent } from './recording-control/recording-control.component';
import {MatSelectModule} from "@angular/material/select";
import {MatIconModule} from "@angular/material/icon";
import {MatFormFieldModule} from "@angular/material/form-field";
import { AboutComponent } from './about/about.component';
import { SetIpComponent } from './set-ip/set-ip.component';
import {MatDialogModule} from "@angular/material/dialog";
import { IdleTimeoutModalComponent } from './idle-timeout-modal/idle-timeout-modal.component';
import { CameraParamsComponent } from './camera-params/camera-params.component';
import { DrawdownCalcContainerComponent } from './drawdown-calc-container/drawdown-calc-container.component';
import {UserIdleModule} from "./angular-user-idle/angular-user-idle.module";
import {MatMenuModule} from "@angular/material/menu";
import { LayoutModule } from '@angular/cdk/layout';
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MatInputModule} from "@angular/material/input";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {MatTooltipModule} from "@angular/material/tooltip";
import { CloudProxyComponent } from './cloud-proxy/cloud-proxy.component';
import { SetUpGuestAccountComponent } from './set-up-guest-account/set-up-guest-account.component';
import {MatDividerModule} from "@angular/material/divider";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {DateAdapter, MAT_DATE_LOCALE, MatNativeDateModule} from '@angular/material/core';
import {Platform} from "@angular/cdk/platform";
import {CustomDateAdapter} from "./cameras/camera.service";
import { CameraAdminPageHostingComponent } from './camera-admin-page-hosting/camera-admin-page-hosting.component';
import {GetLocalWifiDetailsComponent} from './get-local-wifi-details/get-local-wifi-details.component';
import {WifiSettingsComponent} from './wifi-settings/wifi-settings.component';
import {GetActiveIPAddressesComponent} from './get-active-ipaddresses/get-active-ipaddresses.component';
import { CreateUserAccountContainerComponent } from './create-user-account-container/create-user-account-container.component';
import {SharedModule} from "./shared/shared.module";

@NgModule({
    declarations: [
        AppComponent,
        CamerasComponent,
        NavComponent,
        RecordingControlComponent,
        AboutComponent,
        SetIpComponent,
        IdleTimeoutModalComponent,
        CameraParamsComponent,
        DrawdownCalcContainerComponent,
        CloudProxyComponent,
        SetUpGuestAccountComponent,
        CameraAdminPageHostingComponent,
        GetActiveIPAddressesComponent,
        GetLocalWifiDetailsComponent,
        WifiSettingsComponent,
        CreateUserAccountContainerComponent
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        AppRoutingModule,
        BrowserAnimationsModule,
        MatCardModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatCheckboxModule,
        MatButtonModule,
        MatButtonToggleModule,
        MatInputModule,
        MatIconModule,
        MatSelectModule,
        MatDialogModule,
        MatMenuModule,
        MatProgressSpinnerModule,
        FormsModule,
        ReactiveFormsModule,
        // Optionally you can set time for `idle`, `timeout` and `ping` in seconds.
        // Default values: `idle` is 600 (10 minutes), `timeout` is 300 (5 minutes)
        // and `ping` is 6q0 (1 minutes).
        UserIdleModule.forRoot({idle: 600, timeout: 60, ping: 60}),
        LayoutModule,
        MatTableModule,
        MatSortModule,
        MatTooltipModule,
        MatDividerModule,
        MatSlideToggleModule,
        MatDatepickerModule,
        MatNativeDateModule,
        SharedModule],
    exports: [
    ],
    providers: [{
        provide: DateAdapter,
        useClass: CustomDateAdapter,
        deps: [MAT_DATE_LOCALE, Platform]
    },
        HttpClient, BaseUrl, provideHttpClient(withInterceptorsFromDi())]
})
export class AppModule {
}
