import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {RegisterLocalNvrAccountComponent} from './register-local-nvr-account/register-local-nvr-account.component';
import {SetupSMTPClientComponent} from './setup-smtpclient/setup-smtpclient.component';
import {ActivemqCredentialsComponent} from "./activemq-credentials/activemq-credentials.component";

const routes: Routes = [
  {path: 'registerLocalAccount', component: RegisterLocalNvrAccountComponent},
  {path: 'setupSMTPClient', component: SetupSMTPClientComponent},
  {path: 'registerActiveMQAccount', component: ActivemqCredentialsComponent},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
