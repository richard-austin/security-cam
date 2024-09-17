import { NgModule } from '@angular/core';
import {RouterModule, Routes} from "@angular/router";
import {RecordingControlComponent} from "./recording-control.component";

const routes: Routes = [{ path: '', component: RecordingControlComponent }];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RecordingControlRoutingModule { }
