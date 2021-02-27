import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LiveContainerComponent} from "./live-container/live-container.component";

const routes: Routes = [
  {path: 'live', component: LiveContainerComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
