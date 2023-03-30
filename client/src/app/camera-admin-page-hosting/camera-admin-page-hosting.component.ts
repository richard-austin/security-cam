import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {ActivatedRoute} from '@angular/router';
import {Camera} from '../cameras/Camera';
import {ReportingComponent} from '../reporting/reporting.component';
import {interval, Subscription} from 'rxjs';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {environment} from '../../environments/environment';

@Component({
  selector: 'app-camera-admin-page-hosting',
  templateUrl: './camera-admin-page-hosting.component.html',
  styleUrls: ['./camera-admin-page-hosting.component.scss']
})
export class CameraAdminPageHostingComponent implements OnInit, AfterViewInit, OnDestroy {
  cam!: Camera;
  accessToken!: string;
  intervalSubscription!: Subscription;
  hostServiceUrl!: SafeResourceUrl;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  private readonly defaultPort: number =80; // TODO: Probably need to set the port in the camera info as it may not always be 80
  private initialised: boolean = false;
  constructor(private route: ActivatedRoute, private cameraSvc: CameraService, private domSanitizer: DomSanitizer) {
    this.route.paramMap.subscribe((paramMap) => {
      let camera: string = paramMap.get('camera') as string;
      camera = atob(camera);
      let cams = cameraSvc.getCameras()
      cams.forEach((cam) => {
        if (cam.address == camera) {
          this.cam = cam;
        }
      });
      if(this.initialised)
        this.ngOnInit();
    });
  }

  ngOnInit(): void {
    if(this.cam !== undefined)
      this.cameraSvc.getAccessToken(this.cam.address, this.defaultPort).subscribe((result) => {
        this.accessToken = result.accessToken;
        this.hostServiceUrl = this.domSanitizer.bypassSecurityTrustResourceUrl(window.location.protocol+'//'+window.location.hostname+':'+environment.camAdminHostPort+'/?accessToken='+this.accessToken)
        this.intervalSubscription = interval(10000).subscribe(() => {
          this.cameraSvc.resetTimer(this.accessToken).subscribe(() => {},
            error => {
                this.reporting.errorMessage = error;
                this.intervalSubscription.unsubscribe();
            });
        })
      },
      reason => {
        this.reporting.errorMessage = reason;
      })
    this.initialised = true;
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    if(this.intervalSubscription !== undefined)
      this.intervalSubscription.unsubscribe();
  }
}
