import { NgModule } from '@angular/core';
import {RouterModule, Routes} from "@angular/router";
import {WifiSettingsComponent} from "./wifi-settings.component";

const routes: Routes = [{ path: '', component: WifiSettingsComponent }];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class WifiSettingsRoutingModule { }
