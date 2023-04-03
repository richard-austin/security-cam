import {Component, OnInit, ViewChild} from '@angular/core';
import {ReportingComponent} from '../reporting/reporting.component';
import {timer} from 'rxjs';
import { Subscription } from 'rxjs';
import { OnDestroy } from '@angular/core';
import {WifiDetails} from '../shared/wifi-details';
import {WifiUtilsService} from '../shared/wifi-utils.service';
import {UtilsService} from '../shared/utils.service';
import { AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-get-local-wifi-details',
  templateUrl: './get-local-wifi-details.component.html',
  styleUrls: ['./get-local-wifi-details.component.scss']
})
export class GetLocalWifiDetailsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  wifiDetails!: WifiDetails[];
  displayedColumns: string[] = ["InUse", "Ssid", "Rate", "Signal", "Channel", "Security", "Mode", "Bssid"];
  subscription!: Subscription;
  private wifiEnabled: boolean = true;
  private isGuest: boolean = true;

  constructor(private wifiUtilsService: WifiUtilsService, private utils: UtilsService) {
  }

  getLocalWifiDetails(): void {
    this.wifiUtilsService.getLocalWifiDetails().subscribe((result) => {
        this.wifiDetails = result;
        this.wifiDetails = this.wifiDetails.sort((dets1, dets2) => {
          return dets1.in_use ? -1 : parseInt(dets1.signal) < parseInt(dets2.signal) ? 1 : -1;
        })
      },
      reason =>
        this.reporting.errorMessage = reason
    )
  }


  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
  }

  ngAfterViewInit(): void {
    if(!this.isGuest) {
      this.wifiUtilsService.checkWifiStatus().subscribe((result) => {
          this.wifiEnabled = result.status === 'on';
          if (this.wifiEnabled)
            this.subscription = timer(0, 10000).subscribe(() => this.getLocalWifiDetails());
          else
            this.reporting.warningMessage = "Wi-Fi is disabled. You should go to Wi-Fi Admin->Wi-Fi Settings to enable it."
        },
        reason => {
          this.reporting.errorMessage = reason;
        });
    }
    else
      this.reporting.warningMessage = "Not available to guest account";
  }

  ngOnDestroy(): void {
    if(this.subscription !== undefined)
      this.subscription.unsubscribe();
   }
}
