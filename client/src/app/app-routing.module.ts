import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {OnlyClientUsersService} from "./guards/only-client-users.service";
import {canDeactivateGuard} from "./guards/can-deactivate.guard";

const routes: Routes = [
  {path: 'live/:streamName', loadComponent: () => import('./live-container/live-container.component').then(m => m.LiveContainerComponent)},
  {path: 'recording/:streamName', loadComponent: () => import('./recording-control/recording-control.component').then(m => m.RecordingControlComponent)},
  {path: 'multicam', loadComponent: () => import('./multi-cam-view/multi-cam-view.component').then(m => m.MultiCamViewComponent)},
  {path: 'changeemail', loadComponent: () => import('./change-email/change-email.component').then(m => m.ChangeEmailComponent), canActivate: [OnlyClientUsersService]},
  {path: 'changepassword', loadComponent: () => import('./change-password/change-password.component').then(m => m.ChangePasswordComponent), canActivate: [OnlyClientUsersService]},
  {path: 'cameraparams/:camera', loadComponent: () => import('./camera-params/camera-params.component').then(m => m.CameraParamsComponent)},
  {path: 'configsetup', canDeactivate: [canDeactivateGuard], loadComponent: () => import('./config-setup/config-setup.component').then(m => m.ConfigSetupComponent)},
  {path: 'adhochostingconfig', loadComponent: () => import('./ad-hoc-hosting-config/ad-hoc-hosting-config.component').then(m => m.AdHocHostingConfigComponent), canActivate: [OnlyClientUsersService]},
  {path: 'camadmin/:camera', loadComponent: () => import('./camera-admin-page-hosting/camera-admin-page-hosting.component').then(m => m.CameraAdminPageHostingComponent), canActivate: [OnlyClientUsersService]},
  {path: 'getactiveipaddresses', loadComponent: () => import('./get-active-ipaddresses/get-active-ipaddresses.component').then(m => m.GetActiveIPAddressesComponent), canActivate: [OnlyClientUsersService]},
  {path: 'wifisettings', loadComponent: () => import('./wifi-settings/wifi-settings.component').then(m => m.WifiSettingsComponent)},
  {path: 'getlocalwifidetails', loadComponent: () => import('./get-local-wifi-details/get-local-wifi-details.component').then(m => m.GetLocalWifiDetailsComponent)},
  {path: 'about', loadComponent: () => import('./about/about.component').then(m =>  m.AboutComponent)},
  {path: 'setupguestaccount', loadComponent: () => import('./set-up-guest-account/set-up-guest-account.component').then(m => m.SetUpGuestAccountComponent), canActivate: [OnlyClientUsersService]},
  {path: 'setip', loadComponent: () => import('./set-ip/set-ip.component').then(m => m.SetIpComponent), canActivate: [OnlyClientUsersService]},
  {path: 'cloudproxy', loadComponent: () => import('./cloud-proxy/cloud-proxy.component').then(m => m.CloudProxyComponent), canActivate: [OnlyClientUsersService]},
  {path: 'cua', loadComponent: () => import('./create-user-account-container/create-user-account-container.component').then(m => m.CreateUserAccountContainerComponent), canActivate: [OnlyClientUsersService]},
  {path: 'dc', loadComponent: () => import('./drawdown-calc-container/drawdown-calc-container.component').then(m => m.DrawdownCalcContainerComponent)},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
