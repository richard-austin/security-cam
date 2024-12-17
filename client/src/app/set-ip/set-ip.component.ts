import {Component, OnInit, ViewChild} from '@angular/core';
import {MyIp, UtilsService} from "../shared/utils.service";
import {ReportingComponent} from "../reporting/reporting.component";

@Component({
    selector: 'app-set-ip',
    templateUrl: './set-ip.component.html',
    styleUrls: ['./set-ip.component.scss'],
    standalone: false
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
