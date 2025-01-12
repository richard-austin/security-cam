import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {MultiCamViewComponent} from "./multi-cam-view.component";
import {MatIcon} from "@angular/material/icon";
import {MatCard, MatCardContent, MatCardSubtitle, MatCardTitle} from "@angular/material/card";
import {
  MatCell, MatCellDef,
  MatColumnDef,
  MatHeaderCell, MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable
} from "@angular/material/table";
import {MatButton, MatIconButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {MatCheckbox} from "@angular/material/checkbox";
import {SharedModule} from "../shared/shared.module";
import {LiveContainerModule} from "../live-container/live-container.module";
import {MultiCamViewRoutingModule} from "./multi-cam-view-routing.module";
import {MatRadioButton, MatRadioGroup} from "@angular/material/radio";

@NgModule({
  declarations: [
      MultiCamViewComponent,
  ],
  imports: [
    MultiCamViewRoutingModule,
    CommonModule,
    SharedModule,
    MatIcon,
    MatCard,
    MatCardTitle,
    MatCardContent,
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatCell,
    MatIconButton,
    MatTooltip,
    MatCheckbox,
    MatHeaderRow,
    MatRow,
    MatButton,
    LiveContainerModule,
    MatRowDef,
    MatHeaderRowDef,
    MatHeaderCellDef,
    MatCellDef,
    MatCardSubtitle,
    MatRadioGroup,
    MatRadioButton,
  ]
})
export class MultiCamViewModule { }
