import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import {RouterOutlet} from '@angular/router';
import {RegisterLocalNvrAccountComponent} from './register-local-nvr-account/register-local-nvr-account.component';
import {ReportingComponent} from './reporting/reporting.component';
import {MatCardModule} from '@angular/material/card';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatButtonModule} from '@angular/material/button';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatDialogModule} from '@angular/material/dialog';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule} from '@angular/material/tooltip';
import {AppRoutingModule} from './app-routing.module';
import {CommonModule} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import {BaseUrl} from './shared/BaseUrl/BaseUrl';

@NgModule({
  declarations: [
    AppComponent,
    RegisterLocalNvrAccountComponent,
    ReportingComponent
  ],
    imports: [
      CommonModule,
      AppRoutingModule,
      BrowserModule,
      HttpClientModule,
      MatCardModule,
      ReactiveFormsModule,
      MatFormFieldModule,
      MatCheckboxModule,
      MatButtonModule,
      MatButtonToggleModule,
      MatInputModule,
      MatIconModule,
      MatSelectModule,
      MatDialogModule,
      MatMenuModule,
      MatTooltipModule,
      MatProgressSpinnerModule,
      RouterOutlet
    ],
  providers: [BaseUrl],
  bootstrap: [AppComponent]
})
export class AppModule { }
