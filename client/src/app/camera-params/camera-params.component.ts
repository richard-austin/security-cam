import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {UtilsService} from "../shared/utils.service";
import {CameraService} from "../cameras/camera.service";
import {Camera, CameraParams} from "../cameras/Camera";
import {Subscription} from "rxjs";
import {MatSelectChange} from "@angular/material/select";
import {MatSelect} from "@angular/material/select/select";

@Component({
  selector: 'app-camera-params',
  templateUrl: './camera-params.component.html',
  styleUrls: ['./camera-params.component.scss']
})
export class CameraParamsComponent implements OnInit, AfterViewInit, OnDestroy {
  private activeLiveUpdates!: Subscription;
  @ViewChild('irselector') irselector!:MatSelect;
  @ViewChild('startDate') startDate!:ElementRef<HTMLInputElement>;
  @ViewChild('softVersion') softVersion!:ElementRef<HTMLInputElement>;
  @ViewChild('model') model!:ElementRef<HTMLInputElement>;

  constructor(private utils:UtilsService, private cameraSvc:CameraService) { }

  cameraParams!: CameraParams;
  cam!: Camera;
  cameraParamsForm: any;

  private setCamera() {
    this.cam = this.cameraSvc.getActiveLive()[0];
    if(this.cam && this.cam.address!==undefined&&this.cam.controlUri!==undefined) {
      this.utils.cameraParams(this.cam.address, this.cam.controlUri, "cmd=getinfrared&cmd=getserverinfo").subscribe(
        result => {
          this.cameraParams = result;
          // Show the current IR setting
          this.irselector.writeValue(this.cameraParams.infraredstat);
          this.startDate.nativeElement.value = this.cameraParams.startdate;
          this.startDate.nativeElement.disabled = true;
          this.softVersion.nativeElement.value = this.cameraParams.softVersion;
          this.softVersion.nativeElement.disabled = true;
          this.model.nativeElement.value = this.cameraParams.model;
          this.model.nativeElement.disabled = true;
        }
      )
    }
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

  formSubmitted() {

  }

  setSelectedIRStatus($event: MatSelectChange) {

  }
}
