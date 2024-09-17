import { NgModule } from '@angular/core';
import {RouterModule, Routes} from "@angular/router";
import {WifiSettingsComponent} from "./wifi-settings.component";
import {GetLocalWifiDetailsComponent} from "../get-local-wifi-details/get-local-wifi-details.component";

const routes: Routes = [
  {path: 'wifisettings', component: WifiSettingsComponent},
  {path: 'getlocalwifidetails', component: GetLocalWifiDetailsComponent},];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class WifiSettingsRoutingModule { }
