import {Component, OnInit, Signal, viewChild} from '@angular/core';
import {UtilsService, Version} from "../shared/utils.service";
import {ReportingComponent} from "../reporting/reporting.component";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {SharedModule} from "../shared/shared.module";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {HttpErrorResponse} from "@angular/common/http";


@Component({
  selector: 'app-about',
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.scss'],
  imports: [SharedModule, SharedAngularMaterialModule]
})
export class AboutComponent implements OnInit {
  errorReporting: Signal<ReportingComponent> = viewChild.required(ReportingComponent);
  version: string = "Unknown";
  openSourceInfo: string = "Loading...";

  constructor(private utils: UtilsService, private _baseUrl: BaseUrl) {
  }

  getOnvifUrl() {
    return this._baseUrl.getLink('assets/images', 'onvif.png')
  }

  ngOnInit(): void {
    this.utils.getVersion().subscribe({
      next: (version: Version) => {
        this.version = version.version;
      },
      error:
        (reason: HttpErrorResponse) => {
          this.errorReporting().errorMessage = reason;
        }
    });
    this.utils.getOpenSourceInfo().subscribe({
        next: (info: string) => {
          this.openSourceInfo = info;
        },
        error: reason => {
          this.errorReporting().errorMessage = reason;
        }
      }
    );
  }
}
