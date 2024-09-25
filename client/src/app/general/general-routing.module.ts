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
import {OnlyClientUsersService} from "../guards/only-client-users.service";

const routes: Routes = [
  {path: 'setupguestaccount', component: SetUpGuestAccountComponent, canActivate: [OnlyClientUsersService]},
  {path: 'setip', component: SetIpComponent, canActivate: [OnlyClientUsersService]},
  {path: 'cloudproxy', component: CloudProxyComponent, canActivate: [OnlyClientUsersService]},
  {path: 'cua', component: CreateUserAccountContainerComponent, canActivate: [OnlyClientUsersService]},
  {path: 'getactiveipaddresses', component: GetActiveIPAddressesComponent, canActivate: [OnlyClientUsersService]},
  {path: 'dc', component: DrawdownCalcContainerComponent},
  {path: 'camadmin/:camera', component: CameraAdminPageHostingComponent, canActivate: [OnlyClientUsersService]},
  {path: 'about', component: AboutComponent}
];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GeneralRoutingModule {
}
