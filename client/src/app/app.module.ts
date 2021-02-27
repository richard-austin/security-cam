import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CamerasComponent } from './cameras/cameras.component';
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {BaseUrl} from "./shared/BaseUrl/BaseUrl";
import { NavComponent } from './nav/nav.component';
import {FontAwesomeModule} from "@fortawesome/angular-fontawesome";
import { VideoComponent } from './video/video.component';
import { LiveContainerComponent } from './live-container/live-container.component';

@NgModule({
  declarations: [
    AppComponent,
    CamerasComponent,
    NavComponent,
    VideoComponent,
    LiveContainerComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FontAwesomeModule,
  ],
  providers: [HttpClient, BaseUrl],
  bootstrap: [AppComponent]
})
export class AppModule { }
