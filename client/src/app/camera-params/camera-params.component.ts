import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {UtilsService} from "../shared/utils.service";
import {CameraService} from "../cameras/camera.service";
import {Camera, CameraParams} from "../cameras/Camera";
import {Subscription} from "rxjs";
import {ReportingComponent} from "../reporting/reporting.component";
import {AbstractControl, FormControl, FormGroup, Validators} from "@angular/forms";

@Component({
  selector: 'app-camera-params',
  templateUrl: './camera-params.component.html',
  styleUrls: ['./camera-params.component.scss']
})
export class CameraParamsComponent implements OnInit, AfterViewInit, OnDestroy {
  private activeLiveUpdates!: Subscription;
  irselector!: AbstractControl;
  cameraName!: AbstractControl;
  dateFormat!: AbstractControl;
  startDate!: AbstractControl;
  softVersion!: AbstractControl;
  model!: AbstractControl;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  constructor(private utils: UtilsService, private cameraSvc: CameraService) {
  }

  cameraParams!: CameraParams;
  cam!: Camera;
  downloading: boolean = true;
  camControlFormGroup!: FormGroup;

  private setCamera() {
    this.reporting.dismiss();
    this.downloading = true;
    this.cam = this.cameraSvc.getActiveLive()[0];
    if (this.cam && this.cam.address !== undefined && this.cam.controlUri !== undefined) {
      this.utils.cameraParams(this.cam.address, this.cam.controlUri, "cmd=getinfrared&cmd=getserverinfo&cmd=getoverlayattr&-region=0&cmd=getserverinfo&cmd=getoverlayattr&-region=1").subscribe(
        result => {
          this.downloading = false;
          this.cameraParams = result;
          // Show the current IR setting
          this.irselector.setValue(this.cameraParams.infraredstat);
          this.startDate.setValue(this.cameraParams.startdate);
          this.cameraName.setValue(this.cameraParams.name_1);
          this.dateFormat.setValue(this.cameraParams.name_0);
          this.dateFormat.disable(); // Disable this one until I have suitable regex for validation
          this.startDate.disable();
          this.softVersion.setValue(this.cameraParams.softVersion);
          this.softVersion.disable();
          this.model.setValue(this.cameraParams.model);
          this.model.disable();
        },
        reason => {
          this.downloading = false;
          this.reporting.errorMessage = reason;
        }
      )
    }
  }

  updateParams() {
    this.reporting.dismiss();
    this.downloading = true;
    this.utils.setCameraParams(this.cam.address, this.cam.controlUri, this.irselector.value, this.cameraName.value).subscribe(() => {
        this.downloading = false;
        this.reporting.successMessage = "Update Successful"

        // Update the locally stored values
        this.cameraParams.infraredstat = this.irselector.value;
        this.cameraParams.name_1 = this.cameraName.value;
      },
      reason => {
        this.downloading = false;
        this.reporting.errorMessage = reason;
      }
    );
  }

  hasError = (controlName: string, errorName: string): boolean => {
    return this.camControlFormGroup.controls[controlName].hasError(errorName);
  }

  anyInvalid(): boolean {
    return this.irselector.invalid || this.cameraName.invalid;
  }

  ngOnInit(): void {
    this.camControlFormGroup = new FormGroup({
      irselector: new FormControl('', [Validators.required]),
      cameraName: new FormControl('', [Validators.required, Validators.maxLength(25)]),
      dateFormat: new FormControl('', [Validators.required, Validators.maxLength(30)]),
      startDate: new FormControl('', [Validators.required]),
      softVersion: new FormControl('', [Validators.required]),
      model: new FormControl('', [Validators.required])
    });
  }

  ngAfterViewInit(): void {
    this.irselector = this.camControlFormGroup.controls['irselector'];
    this.cameraName = this.camControlFormGroup.controls['cameraName'];
    this.dateFormat = this.camControlFormGroup.controls['dateFormat'];
    this.startDate = this.camControlFormGroup.controls['startDate'];
    this.softVersion = this.camControlFormGroup.controls['softVersion'];
    this.model = this.camControlFormGroup.controls['model'];

    this.activeLiveUpdates = this.cameraSvc.getActiveLiveUpdates().subscribe(() => this.setCamera());
    this.setCamera();
  }

  ngOnDestroy(): void {
    this.activeLiveUpdates.unsubscribe();
  }

  anyChanged() {
    return this.irselector?.value !== this.cameraParams?.infraredstat
      || this.cameraName?.value !== this.cameraParams?.name_1
      || this.dateFormat?.value !== this.cameraParams?.name_0;
  }
}
