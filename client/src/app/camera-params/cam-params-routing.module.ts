import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {CameraParamsComponent} from "./camera-params.component";

const routes: Routes = [{ path: '', component: CameraParamsComponent }];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CamParamsRoutingModule { }
