import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {ActivatedRoute} from '@angular/router';
import {Camera} from '../cameras/Camera';
import {ReportingComponent} from '../reporting/reporting.component';
import {interval, Subscription} from 'rxjs';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {environment} from '../../environments/environment';
import {UtilsService} from "../shared/utils.service";


@Component({
  selector: 'app-camera-admin-page-hosting',
  templateUrl: './camera-admin-page-hosting.component.html',
  imports: [
    ReportingComponent
],
  styleUrls: ['./camera-admin-page-hosting.component.scss']
})
export class CameraAdminPageHostingComponent implements OnInit, AfterViewInit, OnDestroy {
  address!: string;
  accessToken!: string;
  intervalSubscription: Subscription | undefined;
  hostServiceUrl!: SafeResourceUrl;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  private readonly defaultPort: number = 80; // TODO: Probably need to set the port in the camera info as it may not always be 80
  private initialised: boolean = false;

  constructor(private route: ActivatedRoute, private cameraSvc: CameraService, utils: UtilsService, private domSanitizer: DomSanitizer) {
    this.route.paramMap.subscribe((paramMap) => {
      if(this.accessToken !== undefined)
        cameraSvc.closeClients(this.accessToken).subscribe();
      let address: string = paramMap.get('camera') as string;
      address = atob(address);
      // Verify that the address is either for one of the cameras or ad hoc devices
      let cams = cameraSvc.getCameras();
      cams.forEach((cam) => {
        if (cam.address == address) {
          this.address = address;
        }
      });
      if(this.address === undefined || this.address !== address) {
        let devices = utils.adHocDevices;
        devices.forEach((device) => {
          if(device.ipAddress == address) {
            this.address = address;
          }
        })
      }
      if (this.initialised) {
        this.ngOnInit();
      }
    });
  }

  ngOnInit(): void {
    if (this.address !== undefined) {
      this.cameraSvc.getAccessToken(this.address, this.defaultPort).subscribe((result) => {
          this.accessToken = result.accessToken;
          this.hostServiceUrl = this.domSanitizer.bypassSecurityTrustResourceUrl(window.location.protocol + '//' + window.location.hostname + ':' + environment.camAdminHostPort + '/?accessToken=' + this.accessToken);
          this.intervalSubscription?.unsubscribe();
          this.intervalSubscription = interval(10000).subscribe(() => {
            this.cameraSvc.resetTimer(this.accessToken).subscribe(() => {
              },
              error => {
                this.reporting.errorMessage = error;
                this.intervalSubscription?.unsubscribe();
              });
          });
        },
        reason => {
          this.reporting.errorMessage = reason;
        });
    }
    this.initialised = true;
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    this.cameraSvc.closeClients(this.accessToken).subscribe();
    this.intervalSubscription?.unsubscribe();
  }
}
