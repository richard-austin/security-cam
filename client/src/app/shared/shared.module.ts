import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ReportingComponent} from "../reporting/reporting.component";
import {SafeHtmlPipe} from "./safe-html.pipe";
import {VideoComponent} from "../video/video.component";
import {MatCard, MatCardTitle} from "@angular/material/card";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {FormsModule} from "@angular/forms";
import {AudioInputPipe} from "../video/audio-input.pipe";

@NgModule({
  declarations: [
      ReportingComponent,
      SafeHtmlPipe,
      VideoComponent,
      AudioInputPipe
  ],
    imports: [
        CommonModule,
        MatCard,
        MatCardTitle,
        MatTooltip,
        MatIcon,
        FormsModule,
    ],
    exports: [
        ReportingComponent,
        SafeHtmlPipe,
        VideoComponent,
        AudioInputPipe
    ]
})
export class SharedModule { }
