import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import { IPDetails } from '../shared/IPDetails';
import { WifiUtilsService } from '../shared/wifi-utils.service';
import {ReportingComponent} from '../reporting/reporting.component';
import {UtilsService} from "../shared/utils.service";

@Component({
    selector: 'app-get-active-ipaddresses',
    templateUrl: './get-active-ipaddresses.component.html',
    styleUrls: ['./get-active-ipaddresses.component.scss'],
    standalone: false
})
export class GetActiveIPAddressesComponent implements OnInit {
  @ViewChild(ReportingComponent) reporting!: ReportingComponent
  @ViewChild('scrollable_content') scrollableContent!: ElementRef<HTMLElement> | null

  ipDetails!: IPDetails[];
  displayedColumns: string[] = ["IP", "Name", "ConnType", "Device"];

  constructor(private wifiUtilsService: WifiUtilsService, public utils: UtilsService) { }

  ngOnInit(): void {
    this.wifiUtilsService.getActiveIPAddresses().subscribe((result) => {
      this.ipDetails = result;

    },
      reason => {
          this.reporting.errorMessage = reason;
      })
  }
}
