import { NgModule } from '@angular/core';
import {RouterModule, Routes} from "@angular/router";
import {LiveContainerComponent} from "./live-container.component";

const routes: Routes = [{ path: '', component: LiveContainerComponent }];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LiveContainerRoutingModule { }
