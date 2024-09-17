import { NgModule } from '@angular/core';
import {RouterModule, Routes} from "@angular/router";
import {SetIpComponent} from "../set-ip/set-ip.component";
import {SetUpGuestAccountComponent} from "../set-up-guest-account/set-up-guest-account.component";
import {AboutComponent} from "../about/about.component";
import {CameraAdminPageHostingComponent} from "../camera-admin-page-hosting/camera-admin-page-hosting.component";
import {CloudProxyComponent} from "../cloud-proxy/cloud-proxy.component";
import {GetActiveIPAddressesComponent} from "../get-active-ipaddresses/get-active-ipaddresses.component";
import {DrawdownCalcContainerComponent} from "../drawdown-calc-container/drawdown-calc-container.component";
import {
  CreateUserAccountContainerComponent
} from "../create-user-account-container/create-user-account-container.component";

const routes: Routes = [
  {path: 'setupguestaccount', component: SetUpGuestAccountComponent},
  {path: 'setip', component: SetIpComponent},
  {path: 'cloudproxy', component: CloudProxyComponent},
  {path: 'cua', component: CreateUserAccountContainerComponent},
  {path: 'getactiveipaddresses', component: GetActiveIPAddressesComponent},
  {path: 'dc', component: DrawdownCalcContainerComponent},
  {path: 'camadmin/:camera', component: CameraAdminPageHostingComponent},
  {path: 'about', component: AboutComponent}
];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GeneralRoutingModule {
}
