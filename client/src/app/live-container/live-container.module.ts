import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {SharedModule} from "../shared/shared.module";
import {LiveContainerComponent} from "./live-container.component";
import {PTZControlsComponent} from "./ptzcontrols/ptzcontrols.component";
import {PTZButtonComponent} from "./ptzcontrols/ptzbutton/ptzbutton.component";
import {MatIcon} from "@angular/material/icon";
import {MatIconButton, MatMiniFabButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {PresetButtonComponent} from "./ptzcontrols/preset-button/preset-button.component";
import {MatCard, MatCardContent, MatCardSubtitle, MatCardTitle} from "@angular/material/card";
import {MatDivider} from "@angular/material/divider";
import {MatSlideToggle} from "@angular/material/slide-toggle";



@NgModule({
    declarations: [
        LiveContainerComponent,
        PTZControlsComponent,
        PTZButtonComponent,
        PresetButtonComponent,
    ],
    exports: [
    ],
    imports: [
        CommonModule,
        SharedModule,
        MatIcon,
        MatIconButton,
        MatTooltip,
        MatMiniFabButton,
        MatCard,
        MatCardTitle,
        MatCardSubtitle,
        MatCardContent,
        MatDivider,
        MatSlideToggle
    ]
})
export class LiveContainerModule { }
