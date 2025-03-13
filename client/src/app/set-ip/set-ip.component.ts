import {Component, OnInit, ViewChild} from '@angular/core';
import {MyIp, UtilsService} from "../shared/utils.service";
import {ReportingComponent} from "../reporting/reporting.component";
import {SharedModule} from "../shared/shared.module";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";

@Component({
    selector: 'app-set-ip',
    templateUrl: './set-ip.component.html',
    styleUrls: ['./set-ip.component.scss'],
    imports: [SharedModule, SharedAngularMaterialModule]
})
export class SetIpComponent implements OnInit {
  @ViewChild(ReportingComponent) errorReporting!: ReportingComponent;

  myIp: string = "";
  constructor(private utilsService:UtilsService) { }

  ngOnInit(): void {
    this.utilsService.setIp().subscribe((ip:MyIp) =>{
      this.myIp = ip.myIp;
    },
      reason => this.errorReporting.errorMessage = reason)
  }
}
