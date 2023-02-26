import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService, cameraType} from '../cameras/camera.service';
import {Camera, CameraStream} from '../cameras/Camera';
import {ReportingComponent} from '../reporting/reporting.component';
import {HttpErrorResponse} from '@angular/common/http';
import {Subscription} from 'rxjs';
import {IdleTimeoutStatusMessage, Message, messageType, UtilsService} from '../shared/utils.service';
import {MatDialog} from '@angular/material/dialog';
import {IdleTimeoutModalComponent} from '../idle-timeout-modal/idle-timeout-modal.component';
import {MatDialogRef} from '@angular/material/dialog/dialog-ref';
import {UserIdleConfig} from '../angular-user-idle/angular-user-idle.config';
import {UserIdleService} from '../angular-user-idle/angular-user-idle.service';

declare let SockJS: any;
declare let Stomp: any;

@Component({
  selector: 'app-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild(ReportingComponent) errorReporting!: ReportingComponent;
  @ViewChild('navbarCollapse') navbarCollapse!: ElementRef<HTMLDivElement>;

  cameraStreams: CameraStream[] = []; // All camera streams
  cameras: Camera[] = [];
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
  private stompClient: any;
  cameraTypes: typeof cameraType = cameraType;
  stompSubscription: any;

  constructor(private cameraSvc: CameraService, private utilsService: UtilsService, private userIdle: UserIdleService, private dialog: MatDialog) {
  }

  setVideoStream(camStream: CameraStream): void {
    let suuid = 'suuid=';
    let uri = camStream.stream.uri;
    let index = uri.indexOf(suuid);
    let streamName = uri.substring(index + suuid.length);
    window.location.href = '#/live/' + streamName;
  }

  showRecording(camStream: CameraStream): void {
    let suuid = 'suuid=';
    let uri = camStream.stream.recording.recording_src_url;
    let index = uri.indexOf(suuid);
    let streamName = uri.substring(index + suuid.length);
    window.location.href = '#/recording/' + streamName;
  }

  cameraControl(cam: Camera) {
    window.location.href = '#/cameraparams/' + btoa(cam.address);
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

  logOff(logoff: boolean): void {
    this.confirmLogout = false;
    if (logoff) {
      localStorage.setItem('message', "logoff");
      localStorage.removeItem('message');
      window.location.href = 'logoff';
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

  configSetup() {
    window.location.href = '#/configsetup';
  }

  setUpGuestAccount() {
    window.location.href = '#/setupguestaccount';
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

  initializeWebSocketConnection() {
    let serverUrl: string = window.location.origin + '/stomp';

    let ws = new SockJS(serverUrl);
    this.stompClient = Stomp.over(ws);
    this.stompClient.debug = null;
    let that = this;
    this.stompClient.connect({}, () => {
      this.stompSubscription = that.stompClient.subscribe('/topic/logoff', (message: any) => {
        if (message.body) {
          let msgObj = JSON.parse(message.body);
          if (msgObj.message === 'logoff' && this.isGuest) {
            window.location.href = 'logoff';
            console.log(message.body);
          }
        }
      });
    });
  }

  async ngOnInit(): Promise<void> {
    this.cameraStreams = this.cameraSvc.getCameraStreams();
    this.cameras = this.cameraSvc.getCameras();

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
      window.location.href = 'logoff';
    });

    // Gets the core temperature every minute (Raspberry pi only), and keeps the session alive
    this.pingHandle = this.userIdle.ping$.subscribe(() => this.getTemperature());

    this.cameraSvc.getConfigUpdates().subscribe(() => {
      this.cameraStreams = this.cameraSvc.getCameraStreams();
      this.cameras = this.cameraSvc.getCameras();
    });

    try {
      this.isGuest = (await this.utilsService.isGuest()).guestAccount;
    } catch (error) {
      this.isGuest = true;
      console.error('Error calling isGuest = ' + error.error);
    }
    this.initializeWebSocketConnection();

    window.onstorage = (ev: StorageEvent) => {
      let val = ev.newValue;
      if (val === 'logoff') {
        location.href = 'logoff';
      }
    };
  }

  ngAfterViewInit(): void {
    // If the camera service got any errors while getting the camera setup, then we report it here.
    this.cameraSvc.errorEmitter.subscribe((error: HttpErrorResponse) => this.errorReporting.errorMessage = error);
  }

  ngOnDestroy(): void {
    this.pingHandle.unsubscribe();
    this.timerHandle.unsubscribe();
    this.messageSubscription.unsubscribe();
  }
}
