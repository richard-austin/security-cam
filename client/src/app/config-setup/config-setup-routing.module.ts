import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {ConfigSetupComponent} from "./config-setup.component";

const routes: Routes = [{ path: '', component: ConfigSetupComponent }];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ConfigSetupRoutingModule { }
