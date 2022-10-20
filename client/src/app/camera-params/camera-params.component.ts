import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {UtilsService} from "../shared/utils.service";
import {CameraService} from "../cameras/camera.service";
import {CameraParams, CameraStream} from "../cameras/Camera";
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
  cam!: CameraStream;
  downloading: boolean = true;
  camControlFormGroup!: FormGroup;
  isGuest: boolean = true;

  private setCamera() {
    this.reporting.dismiss();
    this.downloading = true;
    this.cam = this.cameraSvc.getActiveLive()[0];
    if (this.cam && this.cam.camera.address !== undefined && this.cam.camera.cameraParamSpecs.uri !== undefined) {
      this.utils.cameraParams(this.cam.camera.address, this.cam.camera.cameraParamSpecs.uri, this.cam.camera.cameraParamSpecs.params).subscribe(
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
          if(reason.status == 401)
          {
             this.reporting.warningMessage = `
             Unauthorised: The credentials for this camera are not correctly set. Please go to General -> Configure Camera Setup and
             click on the shield icon beside the title (Cameras Configuration). You can then set the the user name and
             password which must be set the same on all cameras.
             `;
          }
          else
            this.reporting.errorMessage = reason;
          this.downloading = false;
        }
      )
    }
  }

  updateParams() {
    this.reporting.dismiss();
    this.downloading = true;
    this.utils.setCameraParams(this.cam.camera.address, this.cam.camera.cameraParamSpecs.uri, this.irselector.value, this.cameraName.value).subscribe(() => {
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


  anyChanged() {
    return this.irselector?.value !== this.cameraParams?.infraredstat
      || this.cameraName?.value !== this.cameraParams?.name_1
      || this.dateFormat?.value !== this.cameraParams?.name_0;
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
    this.isGuest = this.utils.isGuestAccount;
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
}
