import { NgModule } from '@angular/core';
import {RouterModule, Routes} from "@angular/router";
import {MultiCamViewComponent} from "./multi-cam-view.component";

const routes: Routes = [{ path: '', component: MultiCamViewComponent }];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MultiCamViewRoutingModule { }
