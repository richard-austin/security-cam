import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CamerasComponent } from './cameras/cameras.component';
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {BaseUrl} from "./shared/BaseUrl/BaseUrl";
import { NavComponent } from './nav/nav.component';
import {FontAwesomeModule, FaIconLibrary} from "@fortawesome/angular-fontawesome";
import { VideoComponent } from './video/video.component';
import { LiveContainerComponent } from './live-container/live-container.component';
import { MultiCamViewComponent } from './multi-cam-view/multi-cam-view.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatCheckboxModule} from "@angular/material/checkbox";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatInputModule} from "@angular/material/input";
import {MatCardModule} from "@angular/material/card";
import {
  faBackward, faFastBackward,
  faFastForward,
  faForward,
  faPause,
  faPlay,
  fas, faStepBackward,
  faStepForward
} from '@fortawesome/free-solid-svg-icons';
import { far } from '@fortawesome/free-regular-svg-icons';
import {faCaretRight} from "@fortawesome/free-solid-svg-icons";
import { RecordingControlComponent } from './recording-control/recording-control.component';
import {MatSelectModule} from "@angular/material/select";
import { ReportingComponent } from './reporting/reporting.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
import {MatIconModule} from "@angular/material/icon";
import {MatFormFieldModule} from "@angular/material/form-field";

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
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FontAwesomeModule,
    BrowserAnimationsModule,
    MatCardModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatCheckboxModule,
    MatButtonModule,
    MatInputModule,
    MatIconModule,
    MatSelectModule,
    FormsModule
  ],
  providers: [HttpClient, BaseUrl],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(faLibrary: FaIconLibrary)
  {
      faLibrary.addIconPacks(fas, far);
      faLibrary.addIcons(faCaretRight, faFastBackward, faStepForward, faStepBackward, faForward, faBackward, faFastForward, faPause, faPlay);
  }
}
