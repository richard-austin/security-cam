import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {UtilsService} from "../shared/utils.service";
import {CameraService} from "../cameras/camera.service";
import {Camera, CameraParams} from "../cameras/Camera";
import {Subscription} from "rxjs";
import {MatSelect} from "@angular/material/select/select";
import {ReportingComponent} from "../reporting/reporting.component";

@Component({
  selector: 'app-camera-params',
  templateUrl: './camera-params.component.html',
  styleUrls: ['./camera-params.component.scss']
})
export class CameraParamsComponent implements OnInit, AfterViewInit, OnDestroy {
  private activeLiveUpdates!: Subscription;
  @ViewChild('irselector') irselector!:MatSelect;
  @ViewChild('cameraName') cameraName!: ElementRef<HTMLInputElement>;
  @ViewChild('dateFormat') dateFormat!: ElementRef<HTMLInputElement>;
  @ViewChild('startDate') startDate!:ElementRef<HTMLInputElement>;
  @ViewChild('softVersion') softVersion!:ElementRef<HTMLInputElement>;
  @ViewChild('model') model!:ElementRef<HTMLInputElement>;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  constructor(private utils:UtilsService, private cameraSvc:CameraService) { }

  cameraParams!: CameraParams;
  cam!: Camera;
  cameraParamsForm: any;

  private setCamera() {
    this.reporting.dismiss();
    this.cam = this.cameraSvc.getActiveLive()[0];
    if(this.cam && this.cam.address!==undefined&&this.cam.controlUri!==undefined) {
      this.utils.cameraParams(this.cam.address, this.cam.controlUri, "cmd=getinfrared&cmd=getserverinfo&cmd=getoverlayattr&-region=0&cmd=getserverinfo&cmd=getoverlayattr&-region=1").subscribe(
        result => {
          this.cameraParams = result;
          // Show the current IR setting
          this.irselector.writeValue(this.cameraParams.infraredstat);
          this.startDate.nativeElement.value = this.cameraParams.startdate;
          this.cameraName.nativeElement.value = this.cameraParams.name_1;
          this.dateFormat.nativeElement.value = this.cameraParams.name_0;
          this.dateFormat.nativeElement.disabled = true; // Disable this one until I have suitable regex for validation
          this.startDate.nativeElement.disabled = true;
          this.softVersion.nativeElement.value = this.cameraParams.softVersion;
          this.softVersion.nativeElement.disabled = true;
          this.model.nativeElement.value = this.cameraParams.model;
          this.model.nativeElement.disabled = true;
        }
      )
    }
  }

  updateParams() {
    this.reporting.dismiss();
    this.utils.setCameraParams(this.cam.address, this.cam.controlUri, this.irselector.value, this.cameraName.nativeElement.value).subscribe(() =>
      {
          this.reporting.successMessage = "Update Successful"
          this.cameraParams.infraredstat=this.irselector.value;   // Update the locally stored value
      },
      reason =>
        this.reporting.errorMessage = reason
    );
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setCamera());
    this.setCamera();
  }

  ngOnDestroy(): void {
    this.activeLiveUpdates.unsubscribe();
  }

  anyChanged() {
    return this.irselector?.value!==this.cameraParams?.infraredstat
    || this.cameraName?.nativeElement?.value !== this.cameraParams?.name_1
    || this.dateFormat?.nativeElement.value !== this.cameraParams?.name_0;
  }
}
