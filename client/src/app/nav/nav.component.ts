import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {faVideo} from '@fortawesome/free-solid-svg-icons';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'app-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit, AfterViewInit {

  @ViewChild(ReportingComponent) errorReporting!:ReportingComponent;
  // Font awesome icons
  faCamera = faVideo;
  cameras: Camera[] = [];
  confirmLogout: boolean = false;

  constructor(private cameraSvc: CameraService) {
  }

  setVideoStream(cam: Camera):void {
      this.cameraSvc.setActiveLive([cam]);
      window.location.href = '#/live';
  }

  showRecording(cam:Camera):void
  {
    this.cameraSvc.setActiveLive([cam]);
    window.location.href = '#/recording';
  }

  multiCamView() {
    window.location.href = '#/multicam';
  }

  confirmLogoff():void
  {
    this.confirmLogout = true;
  }

  logOff(logoff:boolean):void {
    this.confirmLogout = false;

    if(logoff)
      window.location.href = 'logoff';
  }

  ngOnInit(): void {
    this.cameras = this.cameraSvc.getCameras();
  }

  ngAfterViewInit(): void {
    // If the camera service got any errors while getting the camera setup, then we report it here.
    this.cameraSvc.errorEmitter.subscribe((error:HttpErrorResponse) => this.errorReporting.errorMessage = error);
  }

}
