import { NgModule } from '@angular/core';
import {RouterModule, Routes} from "@angular/router";
import {SetUpGuestAccountComponent} from "./set-up-guest-account.component";

const routes: Routes = [{ path: '', component: SetUpGuestAccountComponent }];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SetUpGuestAccountRoutingModule { }
