import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {faVideo} from '@fortawesome/free-solid-svg-icons';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";
import {interval, Subscription} from "rxjs";
import {UtilsService} from "../shared/utils.service";

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
  temperature!: number;
  noTemperature: boolean = true;
  tempAlertClass!: string;

  constructor(private cameraSvc: CameraService, private utilsService: UtilsService) {
  }

  setVideoStream(cam: Camera): void {
    this.cameraSvc.setActiveLive([cam]);
    window.location.href = '#/live';
  }

  showRecording(cam: Camera): void {
    this.cameraSvc.setActiveLive([cam]);
    window.location.href = '#/recording';
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

  tomcatManager() {
    window.location.href = 'manager/html';
  }

  ngOnInit(): void {
    this.cameras = this.cameraSvc.getCameras();
    // Get the initial core temperature
    this.getTemperature();
    // Gets the core temperature every minute (Raspberry pi only), and prevents the session from timing out
    this.pingHandle = interval(60000).subscribe(() => {
      this.getTemperature();
    });
  }

  ngAfterViewInit(): void {
    // If the camera service got any errors while getting the camera setup, then we report it here.
    this.cameraSvc.errorEmitter.subscribe((error: HttpErrorResponse) => this.errorReporting.errorMessage = error);
  }

  ngOnDestroy(): void {
    this.pingHandle.unsubscribe();
  }

}
