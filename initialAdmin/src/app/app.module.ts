import {NgModule, provideZoneChangeDetection} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import {RouterOutlet} from '@angular/router';
import {RegisterLocalNvrAccountComponent} from './register-local-nvr-account/register-local-nvr-account.component';
import {ReportingComponent} from './reporting/reporting.component';
import {MatCardModule as MatCardModule} from '@angular/material/card';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule as MatFormFieldModule} from '@angular/material/form-field';
import {MatCheckboxModule as MatCheckboxModule} from '@angular/material/checkbox';
import {MatButtonModule as MatButtonModule} from '@angular/material/button';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatInputModule as MatInputModule} from '@angular/material/input';
import {MatSelectModule as MatSelectModule} from '@angular/material/select';
import {MatDialogModule as MatDialogModule} from '@angular/material/dialog';
import {MatProgressSpinnerModule as MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule as MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule as MatTooltipModule} from '@angular/material/tooltip';
import {AppRoutingModule} from './app-routing.module';
import {CommonModule} from '@angular/common';
import {provideHttpClient, withFetch, withInterceptorsFromDi} from '@angular/common/http';
import {BaseUrl} from './shared/BaseUrl/BaseUrl';
import {MatDividerModule} from '@angular/material/divider';
import {LayoutModule} from '@angular/cdk/layout';
import { SetupSMTPClientComponent } from './setup-smtpclient/setup-smtpclient.component';
import { ActivemqCredentialsComponent } from './activemq-credentials/activemq-credentials.component';
import {SafeHtmlPipe} from "./reporting/safe-html.pipe";

@NgModule({ declarations: [
        AppComponent,
        RegisterLocalNvrAccountComponent,
        ReportingComponent,
        SafeHtmlPipe,
        SetupSMTPClientComponent,
        ActivemqCredentialsComponent
    ],
    exports: [
        ReportingComponent
    ],
    bootstrap: [AppComponent], imports: [CommonModule,
        AppRoutingModule,
        BrowserModule,
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
        FormsModule,
        LayoutModule,
        MatDividerModule,
        RouterOutlet], providers: [BaseUrl, provideZoneChangeDetection(), provideHttpClient(withInterceptorsFromDi(), withFetch())] })
export class AppModule { }
