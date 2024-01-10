import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {CloudProxyService, IsMQConnected} from './cloud-proxy.service';
import {ReportingComponent} from '../reporting/reporting.component';
import {UtilsService} from '../shared/utils.service';
import {Client, IMessage, StompSubscription} from "@stomp/stompjs";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";

@Component({
  selector: 'app-cloud-proxy',
  templateUrl: './cloud-proxy.component.html',
  styleUrls: ['./cloud-proxy.component.scss']
})
export class CloudProxyComponent implements OnInit, OnDestroy {
  cps: boolean = true;
  cbEnabled: boolean = true;
  isGuest: boolean = true;
  client!: Client;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  private nvrloginstatusSubscription!: StompSubscription;

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

        this.cbEnabled = true;
        this.cpService.isTransportActive().subscribe((status: IsMQConnected) => {
          this.utils.activeMQTransportActive = status.transportActive;
          this.cps = this.utils.cloudProxyRunning = true;
        });
      },
      (reason) => {
        this.reporting.errorMessage = reason;
        this.cbEnabled = true;
      });
  }

  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
    let serverUrl: string = (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/stomp';
    this.client = new Client({
      brokerURL: serverUrl,
      reconnectDelay: 2000,
      heartbeatOutgoing: 120000,
      heartbeatIncoming: 120000,
      onConnect: () => {
        this.nvrloginstatusSubscription = this.client.subscribe('/topic/nvrloginstatus', (message: IMessage) => {
          if (message.body) {
            let msgObj = JSON.parse(message.body);
            switch (msgObj.status) {
              case "working":
                this.reporting.warningMessage = msgObj.message;
                break;
              case "success":
                this.reporting.successMessage = msgObj.message;
              break;
              case "fail":
                this.reporting.errorMessage = new HttpErrorResponse({error: msgObj.message});
                break;
              default:
                this.reporting.errorMessage = new HttpErrorResponse({error: "Unknown message from server"});
            }
            if (msgObj.message === 'logoff' && this.isGuest) {
              window.location.href = 'logoff';
              console.log(message.body);
            }
          }
        });
      },
      debug: () => {
      }
    });
    this.client.activate();
  }

  setCloudProxyStatus($event: MatCheckboxChange) {
    $event.checked ? this.start() : this.stop();
  }

  ngOnDestroy(): void {
    this.nvrloginstatusSubscription.unsubscribe();
  }
}
