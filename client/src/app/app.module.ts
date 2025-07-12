import { NgModule } from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpClient, provideHttpClient, withInterceptorsFromDi, withFetch } from "@angular/common/http";
import {BaseUrl} from "./shared/BaseUrl/BaseUrl";
import { NavComponent } from './nav/nav.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatCheckbox} from "@angular/material/checkbox";
import {ReactiveFormsModule} from "@angular/forms";
import {MatButton, MatIconAnchor} from "@angular/material/button";
import {MatCard, MatCardContent, MatCardSubtitle, MatCardTitle} from "@angular/material/card";
import {MatFormField} from "@angular/material/select";
import {MatIcon} from "@angular/material/icon";
import {MatError, MatHint, MatLabel} from "@angular/material/form-field";
import { IdleTimeoutModalComponent } from './idle-timeout-modal/idle-timeout-modal.component';
import {UserIdleModule} from "./angular-user-idle/angular-user-idle.module";
import {MatMenu, MatMenuItem, MatMenuTrigger} from "@angular/material/menu";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {MatInput} from "@angular/material/input";
import {
    MatCell, MatCellDef,
    MatColumnDef,
    MatHeaderCell, MatHeaderCellDef,
    MatHeaderRow,
    MatRow,
    MatTable,
} from "@angular/material/table";
import {MatTooltip} from "@angular/material/tooltip";
import {SharedModule} from "./shared/shared.module";
import {DateAdapter, MAT_DATE_LOCALE} from "@angular/material/core";
import {CustomDateAdapter} from "./cameras/camera.service";
import {Platform} from "@angular/cdk/platform";
import {RouterOutlet} from "@angular/router";
import {MatDialogActions, MatDialogContent, MatDialogTitle} from "@angular/material/dialog";

@NgModule({
    declarations: [
        AppComponent,
        NavComponent,
        IdleTimeoutModalComponent
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        AppRoutingModule,
        BrowserAnimationsModule,
        // Optionally you can set time for `idle`, `timeout` and `ping` in seconds.
        // Default values: `idle` is 600 (10 minutes), `timeout` is 300 (5 minutes)
        // and `ping` is 6q0 (1 minutes).
        UserIdleModule.forRoot({idle: 600, timeout: 60, ping: 60}),
        SharedModule,
        MatCard,
        MatCardTitle,
        MatCardContent,
        MatCheckbox,
        MatTooltip,
        MatTable,
        MatHeaderCell,
        MatCell,
        MatColumnDef,
        MatHeaderRow,
        MatRow,
        MatIcon,
        MatCellDef,
        MatHeaderCellDef,
        MatProgressSpinner,
        MatIconAnchor,
        MatButton,
        MatMenuTrigger,
        MatMenu,
        MatMenuItem,
        MatCardSubtitle,
        RouterOutlet,
        MatFormField,
        ReactiveFormsModule,
        MatInput,
        MatLabel,
        MatHint,
        MatError, MatDialogTitle, MatDialogContent, MatDialogActions],
    exports: [
    ],
    providers: [
        provideHttpClient(withFetch()),
        {
            provide: DateAdapter,
            useClass: CustomDateAdapter,
            deps: [MAT_DATE_LOCALE, Platform]
        },

        HttpClient, BaseUrl, provideHttpClient(withInterceptorsFromDi())]
})
export class AppModule {
}
