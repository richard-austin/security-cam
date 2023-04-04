import {Component, OnInit, ViewChild} from '@angular/core';
import { IPDetails } from '../shared/IPDetails';
import { WifiUtilsService } from '../shared/wifi-utils.service';
import {ReportingComponent} from '../reporting/reporting.component';

@Component({
  selector: 'app-get-active-ipaddresses',
  templateUrl: './get-active-ipaddresses.component.html',
  styleUrls: ['./get-active-ipaddresses.component.scss']
})
export class GetActiveIPAddressesComponent implements OnInit {
  @ViewChild(ReportingComponent) reporting!: ReportingComponent
  ipDetails!: IPDetails[];
  displayedColumns: string[] = ["IP", "Name", "ConnType", "Device"];

  constructor(private wifiUtilsService: WifiUtilsService) { }

  ngOnInit(): void {
    this.wifiUtilsService.getActiveIPAddresses().subscribe((result) => {
      this.ipDetails = result;

    },
      reason => {
          this.reporting.errorMessage = reason;
      })
  }
}
