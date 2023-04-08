import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {RegisterLocalNvrAccountComponent} from './register-local-nvr-account/register-local-nvr-account.component';

const routes: Routes = [
  {path: 'registerLocalAccount', component: RegisterLocalNvrAccountComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
