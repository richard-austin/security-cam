import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {AboutComponent} from "./about/about.component";
import {SetIpComponent} from "./set-ip/set-ip.component";
import {CameraParamsComponent} from "./camera-params/camera-params.component";
import {DrawdownCalcContainerComponent} from "./drawdown-calc-container/drawdown-calc-container.component";
import {CloudProxyComponent} from './cloud-proxy/cloud-proxy.component';
import {SetUpGuestAccountComponent} from "./set-up-guest-account/set-up-guest-account.component";
import {CameraAdminPageHostingComponent} from './camera-admin-page-hosting/camera-admin-page-hosting.component';
import {WifiSettingsComponent} from './wifi-settings/wifi-settings.component';
import {GetLocalWifiDetailsComponent} from './get-local-wifi-details/get-local-wifi-details.component';
import {GetActiveIPAddressesComponent} from './get-active-ipaddresses/get-active-ipaddresses.component';
import {CreateUserAccountContainerComponent} from './create-user-account-container/create-user-account-container.component';

const routes: Routes = [
  {path: 'live/:streamName', loadChildren: () => import('./live-container/live-container.module').then(m => m.LiveContainerModule)},
  {path: 'recording/:streamName', loadChildren: () => import('./recording-control/rec-control.module').then(m => m.RecControlModule)},
  {path: 'multicam', loadChildren: () => import('./multi-cam-view/multi-cam-view.module').then(m => m.MultiCamViewModule)},
  {path: 'changeemail', loadChildren: () => import('./change-email/change-email.module').then(m => m.ChangeEmailModule)},
  {path: 'changepassword', loadChildren: () => import('./change-password/change-password.module').then(m => m.ChangePasswordModule) }  ,
  {path: 'about', component: AboutComponent},
  {path: 'setip', component: SetIpComponent},
  {path: 'cameraparams/:camera', component: CameraParamsComponent},
  {path: 'camadmin/:camera', component:  CameraAdminPageHostingComponent},
  {path: 'configsetup', loadChildren: () => import('./config-setup/config-setup.module').then(m => m.ConfigSetupModule)},
  {path: 'setupguestaccount', component: SetUpGuestAccountComponent},
  {path: 'cua', component: CreateUserAccountContainerComponent},
  {path: 'dc', component: DrawdownCalcContainerComponent},
  {path: 'cloudproxy', component: CloudProxyComponent},
  {path: 'getactiveipaddresses', component: GetActiveIPAddressesComponent},
  {path: 'getlocalwifidetails', component: GetLocalWifiDetailsComponent},
  {path: 'wifisettings', component: WifiSettingsComponent},

];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
