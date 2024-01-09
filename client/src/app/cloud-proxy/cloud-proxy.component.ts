import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {CloudProxyService, IsMQConnected} from './cloud-proxy.service';
import {ReportingComponent} from '../reporting/reporting.component';
import {UtilsService} from '../shared/utils.service';
import {Subscription, timer} from "rxjs";

@Component({
  selector: 'app-cloud-proxy',
  templateUrl: './cloud-proxy.component.html',
  styleUrls: ['./cloud-proxy.component.scss']
})
export class CloudProxyComponent implements OnInit, OnDestroy {
  cps: boolean = true;
  cbEnabled: boolean = true;
  isGuest: boolean = true;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  constructor(private cpService: CloudProxyService, private utils: UtilsService) {
    cpService.getStatus().subscribe((status: boolean) => {
        this.cps = status;
        this.utils.cloudProxyRunning = status;
      },
      reason => {
        this.reporting.errorMessage = reason;
      });
  }

  stop(): void {
    this.cbEnabled = false;
    this.cpService.stop().subscribe(() => {
        this.cps = this.utils.cloudProxyRunning = false;
        this.cbEnabled = true;
      },
      (reason) => {
        this.reporting.errorMessage = reason;
        this.cbEnabled = true;
      });
  }

  start(): void {
    this.cbEnabled = false;
    this.cpService.start().subscribe(() => {
        this.utils.activeMQTransportActive = true; // Prevent warning flashing up before isTransportActive returns
        this.cps = this.utils.cloudProxyRunning = true;
        this.cbEnabled = true;
        this.cpService.isTransportActive().subscribe((status: IsMQConnected) => {
          this.utils.activeMQTransportActive = status.transportActive;
        });
      },
      (reason) => {
        this.reporting.errorMessage = reason;
        this.cbEnabled = true;
      });
  }

  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
  }

  setCloudProxyStatus($event: MatCheckboxChange) {
    $event.checked ? this.start() : this.stop();
  }

  ngOnDestroy(): void {
  }
}
