import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LiveContainerComponent} from "./live-container/live-container.component";
import {MultiCamViewComponent} from "./multi-cam-view/multi-cam-view.component";
import {RecordingControlComponent} from "./recording-control/recording-control.component";
import {ChangePasswordComponent} from "./change-password/change-password.component";
import {AboutComponent} from "./about/about.component";

const routes: Routes = [
  {path: 'live', component: LiveContainerComponent},
  {path: 'recording', component: RecordingControlComponent},
  {path: 'multicam', component: MultiCamViewComponent},
  {path: 'changepassword', component: ChangePasswordComponent},
  {path: 'about', component: AboutComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
