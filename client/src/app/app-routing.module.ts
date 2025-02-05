import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {OnlyClientUsersService} from "./guards/only-client-users.service";

const routes: Routes = [
  {path: 'live/:streamName', loadComponent: () => import('./live-container/live-container.component').then(m => m.LiveContainerComponent)},
  {path: 'recording/:streamName', loadComponent: () => import('./recording-control/recording-control.component').then(m => m.RecordingControlComponent)},
  {path: 'multicam', loadComponent: () => import('./multi-cam-view/multi-cam-view.component').then(m => m.MultiCamViewComponent)},
  {path: 'changeemail', loadComponent: () => import('./change-email/change-email.component').then(m => m.ChangeEmailComponent), canActivate: [OnlyClientUsersService]},
  {path: 'changepassword', loadComponent: () => import('./change-password/change-password.component').then(m => m.ChangePasswordComponent), canActivate: [OnlyClientUsersService]},
  {path: 'cameraparams/:camera', loadComponent: () => import('./camera-params/camera-params.component').then(m => m.CameraParamsComponent)},
  {path: 'configsetup', loadComponent: () => import('./config-setup/config-setup.component').then(m => m.ConfigSetupComponent)},
  {path: 'camadmin/:camera', loadComponent: () => import('./camera-admin-page-hosting/camera-admin-page-hosting.component').then(m => m.CameraAdminPageHostingComponent), canActivate: [OnlyClientUsersService]},
  {path: 'getactiveipaddresses', loadComponent: () => import('./get-active-ipaddresses/get-active-ipaddresses.component').then(m => m.GetActiveIPAddressesComponent), canActivate: [OnlyClientUsersService]},
  {path: 'general', loadChildren: () => import('./general/general.module').then(m => m.GeneralModule)},
  {path: 'wifi', loadChildren: () => import('./wifi-settings/wifi-settings.module').then(m => m.WifiSettingsModule), canActivate: [OnlyClientUsersService]},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
