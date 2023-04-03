import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import { IPDetails } from '../shared/IPDetails';
import { WifiUtilsService } from '../shared/wifi-utils.service';
import {ReportingComponent} from '../reporting/reporting.component';
import { UtilsService } from '../shared/utils.service';

@Component({
  selector: 'app-get-active-ipaddresses',
  templateUrl: './get-active-ipaddresses.component.html',
  styleUrls: ['./get-active-ipaddresses.component.scss']
})
export class GetActiveIPAddressesComponent implements OnInit, AfterViewInit {
  @ViewChild(ReportingComponent) reporting!: ReportingComponent
  ipDetails!: IPDetails[];
  displayedColumns: string[] = ["IP", "Name", "ConnType", "Device"];

  constructor(private wifiUtilsService: WifiUtilsService, private utils: UtilsService) { }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    if (!this.utils.isGuestAccount) {
      this.wifiUtilsService.getActiveIPAddresses().subscribe((result) => {
          this.ipDetails = result;

        },
        reason => {
          this.reporting.errorMessage = reason;
        })
    }
    else
      this.reporting.warningMessage = "Not available to the guest account";
  }
}
