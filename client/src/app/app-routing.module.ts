import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {OnlyClientUsersService} from "./guards/only-client-users.service";

const routes: Routes = [
  {path: 'live/:streamName', loadChildren: () => import('./live-container/live-container.module').then(m => m.LiveContainerModule)},
  {path: 'recording/:streamName', loadChildren: () => import('./recording-control/recording-control.module').then(m => m.RecordingControlModule)},
  {path: 'multicam', loadChildren: () => import('./multi-cam-view/multi-cam-view.module').then(m => m.MultiCamViewModule)},
  {path: 'changeemail', loadChildren: () => import('./change-email/change-email.module').then(m => m.ChangeEmailModule), canActivate: [OnlyClientUsersService]},
  {path: 'changepassword', loadChildren: () => import('./change-password/change-password.module').then(m => m.ChangePasswordModule), canActivate: [OnlyClientUsersService]},
  {path: 'cameraparams/:camera', loadComponent: () => import('./camera-params/camera-params.component').then(m => m.CameraParamsComponent)},
  {path: 'configsetup', loadComponent: () => import('./config-setup/config-setup.component').then(m => m.ConfigSetupComponent)},
  {path: 'general', loadChildren: () => import('./general/general.module').then(m => m.GeneralModule)},
  {path: 'wifi', loadChildren: () => import('./wifi-settings/wifi-settings.module').then(m => m.WifiSettingsModule), canActivate: [OnlyClientUsersService]},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
