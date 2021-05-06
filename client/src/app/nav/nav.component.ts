import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {faVideo} from '@fortawesome/free-solid-svg-icons';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";
import {Subscription} from "rxjs";
import {IdleTimeoutStatusMessage, Message, messageType, UtilsService} from "../shared/utils.service";
import {UserIdleService} from "angular-user-idle";
import {MatDialog} from "@angular/material/dialog";
import {IdleTimeoutModalComponent} from "../idle-timeout-modal/idle-timeout-modal.component";
import {UserIdleConfig} from "angular-user-idle/lib/angular-user-idle.config";
import {MatDialogRef} from "@angular/material/dialog/dialog-ref";

@Component({
  selector: 'app-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild(ReportingComponent) errorReporting!: ReportingComponent;
  // Font awesome icons
  faCamera = faVideo;
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

  constructor(private cameraSvc: CameraService, private utilsService: UtilsService, private userIdle: UserIdleService, private dialog: MatDialog) {
  }

  setVideoStream(cam: Camera): void {
    this.cameraSvc.setActiveLive([cam]);
    window.location.href = '#/live';
  }

  showRecording(cam: Camera): void {
    this.cameraSvc.setActiveLive([cam]);
    window.location.href = '#/recording';
  }

  cameraControl(cam: Camera) {
    this.cameraSvc.setActiveLive([cam]);
    window.location.href = '#/cameraparams';
  }

  changePassword() {
    window.location.href = '#/changepassword';
  }

  multiCamView() {
    window.location.href = '#/multicam';
  }

  confirmLogoff(): void {
    this.confirmLogout = true;
  }

  logOff(logoff: boolean): void {
    this.confirmLogout = false;

    if (logoff)
      window.location.href = 'logoff';
  }

  about() {
    window.location.href = '#/about';
  }

  private getTemperature():void
  {
    this.utilsService.getTemperature().subscribe((tmp) => {
        let temperature: string = tmp.temp;
        let idx1: number = temperature.indexOf('=');
        let idx2: number = temperature.lastIndexOf('\'');
        if (idx1 !== -1 && idx2 !== -1) {
          let strTemp: string = temperature.substr(idx1 + 1, idx2 - idx1);
          this.temperature = parseFloat(strTemp);
          this.noTemperature = false;
          if (this.temperature < 50)
            this.tempAlertClass = 'alert-success';
          else if (this.temperature < 70)
            this.tempAlertClass = 'alert-warning';
          else
            this.tempAlertClass = 'alert-danger'
        } else
          this.noTemperature = true;
      },
      () => {
        this.noTemperature = true;
      });
  }

  setIp() {
    window.location.href = '#/setip';
  }

  drawdownCalc() {
    window.location.href = 'dc';
  }

  openIdleTimeoutDialog(idle:number, timeout:number, count:number): void {
    let data:any = {};
    let remainingSecs: number = timeout-count;
     if(remainingSecs === timeout-1) {
     this.idleTimeoutDialogRef = this.dialog.open(IdleTimeoutModalComponent, {
   //     width: '450px',
        data: {idle: idle, remainingSecs: remainingSecs}
      });

       // this.idleTimeoutDialogRef.afterClosed().subscribe(res => {
       // });
    }
    else
    {
      data =  this.idleTimeoutDialogRef.componentInstance.data;
      data.idle = idle;
      data.remainingSecs = remainingSecs;
    }
  }

  ngOnInit(): void {
    this.cameras = this.cameraSvc.getCameras();
    // Get the initial core temperature
    this.getTemperature();

    //Start watching for user inactivity.
    this.userIdle.startWatching();

    this.messageSubscription = this.utilsService.getMessages().subscribe((message:Message) => {
      if(message.messageType === messageType.idleTimeoutStatus)
      {
        let itos: IdleTimeoutStatusMessage = message as IdleTimeoutStatusMessage;
        this.idleTimeoutActive = itos.active;
    //    console.log("idle active = "+this.idleTimeoutActive)
      }
    })
    // Start watching when user idle is starting.
    this.timerHandle = this.userIdle.onTimerStart().subscribe((count) =>{
      if(this.idleTimeoutActive) {
        let config: UserIdleConfig = this.userIdle.getConfigValue();
        // @ts-ignore
        this.openIdleTimeoutDialog(config.idle, config.timeout, count);
      }
      else
        this.userIdle.resetTimer();
    });

    // Log off when time is up.
    this.userIdle.onTimeout().subscribe(() =>
    {
      this.idleTimeoutDialogRef.close();
      window.location.href = 'logoff';
    });

    // Gets the core temperature every minute (Raspberry pi only), and keeps the session alive
    this.pingHandle = this.userIdle.ping$.subscribe(() => this.getTemperature());
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
