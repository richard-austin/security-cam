import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CamerasComponent } from './cameras/cameras.component';
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {BaseUrl} from "./shared/BaseUrl/BaseUrl";
import { NavComponent } from './nav/nav.component';
import { VideoComponent } from './video/video.component';
import { LiveContainerComponent } from './live-container/live-container.component';
import { MultiCamViewComponent } from './multi-cam-view/multi-cam-view.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatCheckboxModule} from "@angular/material/checkbox";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatButtonToggleModule} from "@angular/material/button-toggle";
import {MatInputModule} from "@angular/material/input";
import {MatCardModule} from "@angular/material/card";
import { RecordingControlComponent } from './recording-control/recording-control.component';
import {MatSelectModule} from "@angular/material/select";
import { ReportingComponent } from './reporting/reporting.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
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
import {MatListModule} from "@angular/material/list";
import { LayoutModule } from '@angular/cdk/layout';
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";

@NgModule({
  declarations: [
    AppComponent,
    CamerasComponent,
    NavComponent,
    VideoComponent,
    LiveContainerComponent,
    MultiCamViewComponent,
    RecordingControlComponent,
    ReportingComponent,
    ChangePasswordComponent,
    AboutComponent,
    SetIpComponent,
    IdleTimeoutModalComponent,
    CameraParamsComponent,
    DrawdownCalcContainerComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    MatCardModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatCheckboxModule,
    MatButtonModule,
    MatButtonToggleModule,
//    MatInputModule,
    MatIconModule,
    MatSelectModule,
    MatDialogModule,
    MatMenuModule,
    MatIconModule,
    MatProgressSpinnerModule,
    FormsModule,
    // Optionally you can set time for `idle`, `timeout` and `ping` in seconds.
    // Default values: `idle` is 600 (10 minutes), `timeout` is 300 (5 minutes)
    // and `ping` is 6q0 (1 minutes).
    UserIdleModule.forRoot({idle: 600, timeout: 60, ping: 60}),
    LayoutModule
  ],
  providers: [HttpClient, BaseUrl],
  bootstrap: [AppComponent],
  entryComponents: [IdleTimeoutModalComponent]
})
export class AppModule {
}
