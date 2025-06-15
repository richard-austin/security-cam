import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService, cameraType} from '../cameras/camera.service';
import {Camera, Stream} from '../cameras/Camera';
import {ReportingComponent} from '../reporting/reporting.component';
import { HttpErrorResponse } from '@angular/common/http';
import {Subscription, timer} from 'rxjs';
import {IdleTimeoutStatusMessage, Message, messageType, UtilsService} from '../shared/utils.service';
import {MatDialog} from '@angular/material/dialog';
import {IdleTimeoutModalComponent} from '../idle-timeout-modal/idle-timeout-modal.component';
import {MatDialogRef} from '@angular/material/dialog';
import {UserIdleConfig} from '../angular-user-idle/angular-user-idle.config';
import {UserIdleService} from '../angular-user-idle/angular-user-idle.service';
import {Client, IMessage, StompSubscription} from "@stomp/stompjs";
import {CloudProxyService, IsMQConnected} from "../cloud-proxy/cloud-proxy.service";
import {MatCheckbox} from "@angular/material/checkbox";

@Component({
    selector: 'app-nav',
    templateUrl: './nav.component.html',
    styleUrls: ['./nav.component.scss'],
    standalone: false
})
export class NavComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  @ViewChild('navbarCollapse') navbarCollapse!: ElementRef<HTMLDivElement>;
 @ViewChild('hardwareDecodingCheckBox') hardwareDecodingCheckBox!: MatCheckbox
//  cameras: Map<string, Camera> = new Map<string, Camera>();
  confirmLogout: boolean = false;
  pingHandle!: Subscription;
  timerHandle!: Subscription;
  temperature!: number;
  noTemperature: boolean = true;
  tempAlertClass!: string;
  idleTimeoutDialogRef!: MatDialogRef<IdleTimeoutModalComponent>;
  private idleTimeoutActive: boolean = true;
  private messageSubscription!: Subscription;
  isGuest: boolean = true;
  private client!: Client;
  cameraTypes: typeof cameraType = cameraType;
  logoffSubscription!: StompSubscription;
  talkOffSubscription!: StompSubscription;
  transportWarningSubscription!: StompSubscription;

  constructor(public cameraSvc: CameraService, public utilsService: UtilsService, private userIdle: UserIdleService, private dialog: MatDialog, private cpService: CloudProxyService) {
    this.cpService.getStatus().subscribe((status: boolean) => {
        this.utilsService.cloudProxyRunning = status;
      },
      reason => {
        this.reporting.errorMessage = reason;
      });
  }

  setVideoStream(cam: Camera, stream: Stream): void {
    let suuid = 'suuid=';
    let uri = stream.uri;
    let index = uri.indexOf(suuid);
    let streamName = uri.substring(index + suuid.length);
    window.location.href = '#/live/' + streamName;
  }

  showRecording(cam: Camera, stream: Stream): void {
    let suuid = 'suuid=';
    let uri = stream.recording.recording_src_url;
    let index = uri.indexOf(suuid);
    let streamName = uri.substring(index + suuid.length, uri.length-1);
    window.location.href = '#/recording/' + streamName;
  }

  cameraControl(cam: Camera) {
    window.location.href = '#/cameraparams/' + btoa(cam.address);
  }

  cameraAdmin(cam: Camera) {
    window.location.href = '#/camadmin/' + btoa(cam.address);
  }

  changePassword() {
    window.location.href = '#/changepassword';
  }

  changeEmail() {
    window.location.href = '#/changeemail';
  }

  multiCamView() {
    window.location.href = '#/multicam';
  }

  confirmLogoff(): void {
    this.confirmLogout = true;
  }

  hardwareDecoding(checked: boolean) {
      this.setCookie("hardwareDecoding", checked ? "true" : "false", 600);
  }

  setCookie(cname:string, cvalue:string, exdays:number) {
    const d = new Date();
    d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
    let expires = "expires="+d.toUTCString();
    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
  }

  static getCookie(cname:string) {
    let name = cname + "=";
    let ca = document.cookie.split(';');
    for(let i = 0; i < ca.length; i++) {
      let c = ca[i];
      while (c.charAt(0) == ' ') {
        c = c.substring(1);
      }
      if (c.indexOf(name) == 0) {
        return c.substring(name.length, c.length);
      }
    }
    return "";
  }

  logOff(logoff: boolean): void {
    this.confirmLogout = false;
    if (logoff) {
      localStorage.setItem('message', "logoff");
      localStorage.removeItem('message');
      window.location.href = 'logout';
    }
  }

  about() {
    window.location.href = '#/about';
  }

  private getTemperature(): void {
    this.utilsService.getTemperature().subscribe((tmp) => {
        let temperature: string = tmp.temp;
        let idx1: number = temperature.indexOf('=');
        let idx2: number = temperature.lastIndexOf('\'');
        if (idx1 !== -1 && idx2 !== -1) {
          let strTemp: string = temperature.substr(idx1 + 1, idx2 - idx1);
          this.temperature = parseFloat(strTemp);
          this.noTemperature = false;
          if (this.temperature < 50) {
            this.tempAlertClass = 'success';
          } else if (this.temperature < 70) {
            this.tempAlertClass = 'warning';
          } else {
            this.tempAlertClass = 'danger';
          }
        } else {
          this.noTemperature = false;
        }
      },
      () => {
        this.noTemperature = true;
        this.tempAlertClass = 'alert-danger';
      });
  }

  setIp() {
    window.location.href = '#/setip';
  }

  cloudProxy() {
    window.location.href = '#/cloudproxy';
  }

  drawdownCalc() {
    window.location.href = '#/dc';
  }

  admin() {
    window.location.href = '#/cua';
  }

  configSetup() {
    window.location.href = '#/configsetup';
  }

  setUpGuestAccount() {
    window.location.href = '#/setupguestaccount';
  }

  getLocalWifiDetails() {
    window.location.href = '#/getlocalwifidetails';
  }

  wifiSettings() {
    window.location.href = '#/wifisettings';
  }

  getActiveIPAddresses() {
    window.location.href = '#/getactiveipaddresses';
  }

  openIdleTimeoutDialog(idle: number, timeout: number, count: number): void {
    let data: any = {};
    let remainingSecs: number = timeout - count;
    if (remainingSecs === timeout - 1) {
      this.idleTimeoutDialogRef = this.dialog.open(IdleTimeoutModalComponent, {
        //  width: '450px',
        data: {idle: idle, remainingSecs: remainingSecs}
      });

      // this.idleTimeoutDialogRef.afterClosed().subscribe(res => {
      // });
    } else {
      data = this.idleTimeoutDialogRef.componentInstance.data;
      data.idle = idle;
      data.remainingSecs = remainingSecs;
    }
  }

  toggleMenu() {
    let navbarCollapse: HTMLDivElement = this.navbarCollapse.nativeElement;
    let style: string | null = navbarCollapse.getAttribute('style');

    if (style === null || style === 'max-height: 0') {
      navbarCollapse.setAttribute('style', 'max-height: 200px');
    } else {
      navbarCollapse.setAttribute('style', 'max-height: 0');
    }
  }

  menuClosed() {
    let navbarCollapse: HTMLDivElement = this.navbarCollapse.nativeElement;
    navbarCollapse.setAttribute('style', 'max-height: 0');
  }

  /**
   * initializeWebSocketConnection: Set up the websocket connection which logs off guest users when the guest account
   *                                is disabled.
   */
  initializeWebSocketConnection() {
    let serverUrl: string = (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/stomp';

    this.client = new Client({
      brokerURL: serverUrl,
      reconnectDelay: 2000,
      heartbeatOutgoing: 120000,
      heartbeatIncoming: 120000,
      onConnect: () => {
        this.logoffSubscription = this.client.subscribe('/topic/logoff', (message: IMessage) => {
          if (message.body) {
            let msgObj = JSON.parse(message.body);
            if (msgObj.message === 'logoff' && this.isGuest) {
              window.location.href = 'logout';
              console.log(message.body);
            }
          }
        });
        this.talkOffSubscription = this.client.subscribe('/topic/talkoff', (message: IMessage) => this.utilsService.talkOff(message));
        this.transportWarningSubscription = this.client.subscribe('/topic/transportStatus', (message: IMessage) => this.utilsService.setTransportStatus(message));
      },
      debug: () => {
      }
    });
    this.client.activate();
  }

  get cameras(): Map<string, Camera> {
    return this.cameraSvc.getCameras();
  }

  async ngOnInit(): Promise<void> {
    // Get the initial core temperature
    this.getTemperature();

    //Start watching for user inactivity.
    this.userIdle.startWatching();
    this.userIdle.resetTimer();

    this.messageSubscription = this.utilsService.getMessages().subscribe((message: Message) => {
      if (message.messageType === messageType.idleTimeoutStatus) {
        let itos: IdleTimeoutStatusMessage = message as IdleTimeoutStatusMessage;
        this.idleTimeoutActive = itos.active;
        //    console.log("idle active = "+this.idleTimeoutActive)
      }
    });
    // Start watching when user idle is starting.
    this.timerHandle = this.userIdle.onTimerStart().subscribe((count: number) => {
      if (this.idleTimeoutActive) {
        let config: UserIdleConfig = this.userIdle.getConfigValue();
        // @ts-ignore
        this.openIdleTimeoutDialog(config.idle, config.timeout, count);
      } else {
        this.userIdle.resetTimer();
      }
    });

    // Log off when time is up.
    this.userIdle.onTimeout().subscribe(() => {
      this.idleTimeoutDialogRef.close();
      this.logOff(true);
    });

    // Gets the core temperature every minute (Raspberry pi only), and keeps the session alive
    this.pingHandle = this.userIdle.ping$.subscribe(() => this.getTemperature());

    try {
      this.isGuest = (await this.utilsService.isGuest()).guestAccount;
    } catch (error) {
      this.isGuest = true;
      // @ts-ignore
      console.error('Error calling isGuest = ' + error.e);
    }
    this.initializeWebSocketConnection();

    if (!this.isGuest) {
      this.cpService.isTransportActive().subscribe((status: IsMQConnected) => {
            this.utilsService.activeMQTransportActive = status.transportActive;
          },
          reason => {
            this.reporting.errorMessage = reason;
          });
    }

    window.onstorage = (ev: StorageEvent) => {
      let val = ev.newValue;
      if (val === 'logoff') {
        location.href = 'logout';
      }
    };
  }

  ngAfterViewInit(): void {
    let hwdc = NavComponent.getCookie("hardwareDecoding");
    if (hwdc === "") {
        this.setCookie("hardwareDecoding", "true", 600);
        hwdc = "true";
    }
    const sub = timer(30).subscribe(() => {
        sub.unsubscribe();
        this.hardwareDecodingCheckBox.checked = hwdc === "true";
    });

    // If the camera service got any errors while getting the camera setup, then we report it here.
    this.cameraSvc.errorEmitter.subscribe((error: HttpErrorResponse) => this.reporting.errorMessage = error);
  }

  ngOnDestroy(): void {
    this.pingHandle.unsubscribe();
    this.timerHandle.unsubscribe();
    this.messageSubscription.unsubscribe();
    this.logoffSubscription.unsubscribe();
    this.talkOffSubscription.unsubscribe();
    this.client.deactivate({force: false}).then(() => {
    });
  }
}
