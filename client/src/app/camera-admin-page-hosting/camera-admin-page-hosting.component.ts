import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {ActivatedRoute} from '@angular/router';
import {ReportingComponent} from '../reporting/reporting.component';
import {interval, Subscription} from 'rxjs';
import {SafeResourceUrl} from '@angular/platform-browser';
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
  intervalSubscription: Subscription | undefined;
  hostServiceUrl!: SafeResourceUrl;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  private webAdminPort: number = 80;
  private initialised: boolean = false;
  private tabHandle: Window | null = null;

  constructor(private route: ActivatedRoute, private cameraSvc: CameraService, utils: UtilsService) {
    this.route.paramMap.subscribe((paramMap) => {
      cameraSvc.closeClient().subscribe();
      let address: string = paramMap.get('camera') as string;
      address = atob(address);
      // Verify that the address is either for one of the cameras or ad hoc devices
      let cams = cameraSvc.getCameras();
      cams.forEach((cam) => {
        if (cam.address === address) {
          this.address = address;
          this.webAdminPort = 80;   // TODO: Probably need to set the port in the camera info
        }
      });
      if (this.address === undefined || this.address !== address) {
        let devices = utils.adHocDevices;
        devices.forEach((device) => {
          if (device.ipAddress === address) {
            this.address = address;
            this.webAdminPort = device.ipPort;
          }
        })
      }
      if (this.initialised) {
        this.ngOnInit();
      }
    });
  }

  makeId(length: number) {
    let result = '';
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    const charactersLength = characters.length;
    for (var i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
  }


  ngOnInit(): void {
    if (this.address !== undefined) {
      this.cameraSvc.getHostingAccess(this.address, this.webAdminPort).subscribe(() => {
          // this.hostServiceUrl = this.domSanitizer.bypassSecurityTrustResourceUrl('http://' + window.location.hostname + ':' + environment.camAdminHostPort + '/?randomId=' + this.makeId(12));
          if (this.tabHandle)
            this.tabHandle.close();

          this.tabHandle = window.open('http://' + window.location.hostname + ':' + environment.camAdminHostPort + '/?randomId=' + this.makeId(12), '_blank');
          this.intervalSubscription?.unsubscribe();
          this.intervalSubscription = interval(10000).subscribe(() => {
            this.cameraSvc.resetTimer().subscribe(() => {
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
    this.cameraSvc.closeClient().subscribe();
    this.intervalSubscription?.unsubscribe();
    if (this.tabHandle)
      this.tabHandle.close();
  }
}
