import {Component, OnInit, ViewChild} from '@angular/core';
import {UtilsService, Version} from "../shared/utils.service";
import {ReportingComponent} from "../reporting/reporting.component";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";

@Component({
  selector: 'app-about',
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.scss']
})
export class AboutComponent implements OnInit {
  @ViewChild(ReportingComponent) errorReporting!: ReportingComponent;
  version: string = "Unknown";

  constructor(private utils: UtilsService, private _baseUrl: BaseUrl) {
  }

  getOnvifUrl() {
    return this._baseUrl.getLink('assets', 'onvif.png')
  }

  ngOnInit(): void {
    this.utils.getVersion().subscribe((version: Version) => {
        this.version = version.version;
      },
      reason => this.errorReporting.errorMessage = reason
      );
  }

}
