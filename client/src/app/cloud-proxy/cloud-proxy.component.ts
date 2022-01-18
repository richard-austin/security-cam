import {Component, OnInit, ViewChild} from '@angular/core';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {CloudProxyService} from './cloud-proxy.service';
import {ReportingComponent} from '../reporting/reporting.component';

@Component({
  selector: 'app-cloud-proxy',
  templateUrl: './cloud-proxy.component.html',
  styleUrls: ['./cloud-proxy.component.scss']
})
export class CloudProxyComponent implements OnInit {
  cps: boolean = true;
  cbEnabled: boolean = true;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  constructor(private cpService: CloudProxyService) {
    cpService.getStatus().subscribe((status: boolean) => {
      this.cps = status;
    },
      reason => {
        this.reporting.errorMessage = reason;
      });
  }

  stop(): void {
    this.cbEnabled = false;
    this.cpService.stop().subscribe(() => {
        this.cps = false;
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
        this.cps = true;
        this.cbEnabled = true;
      },
      (reason) => {
        this.reporting.errorMessage = reason;
        this.cbEnabled = true;
      });
  }

  ngOnInit(): void {
  }

  setCloudProxyStatus($event: MatCheckboxChange) {
    $event.checked ? this.start() : this.stop();
  }
}
