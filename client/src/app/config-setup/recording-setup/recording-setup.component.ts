import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SharedModule} from "../../shared/shared.module";
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";
import {ReportingComponent} from "../../reporting/reporting.component";
import {Camera, Stream} from "../../cameras/Camera";
import {UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {MatSelectChange} from "@angular/material/select";
import {CameraService} from "../../cameras/camera.service";

@Component({
  selector: 'app-recording-setup',
  templateUrl: './recording-setup.component.html',
  styleUrl: './recording-setup.component.scss',
  imports: [SharedModule, SharedAngularMaterialModule]
})
export class RecordingSetupComponent implements OnInit, AfterViewInit {
  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent
  @Input() camera!: Camera | undefined | null;
  localCamera!: Camera;

  formGroup!: UntypedFormGroup;

  constructor(public cameraSvc: CameraService) {
  }

  setRecordingType($event: MatSelectChange) {
    this.localCamera.recordingType = $event.value;
  }

  setRetriggerWindow($event: MatSelectChange) {
    this.localCamera.retriggerWindow = $event.value;
  }

  setSelectedStream($event: MatSelectChange) {
    this.localCamera.ftp = $event.value;
  }

  setThreshold($event: Event, stream: Stream) {
    if (stream.motion.enabled) {
      let input: HTMLInputElement = $event.target as HTMLInputElement;
      stream.motion.threshold = Number(input.value);
    }
  }

  setRecordingTrigger($event: MatSelectChange, stream: Stream) {
    if (stream.motion.enabled) {
      stream.motion.trigger_recording_on = $event.value;
      // this.FixUpCamerasData();
    }
  }


  cancel() {
    this.hideDialogue.emit();
  }

  anyInvalid(): boolean {
    return this.localCamera.recordingType === 'ftp' && this.localCamera.ftp === 'none';
  }

  confirmChanges() {
    if (this.camera !== undefined && this.camera !== null) {
      Object.assign(this.camera, this.localCamera)
      this.hideDialogue.emit();
    }
  }

  clone(cam: Camera) : Camera {
     return window.structuredClone(cam); // { ... cam };
  }

  ngOnInit(): void {
    if (this.camera !== undefined && this.camera !== null) {
      this.localCamera = this.clone(this.camera);

      this.formGroup = new UntypedFormGroup({
        recordingType: new UntypedFormControl(this.localCamera.recordingType, [Validators.required]),
        ftpStreamSelect: new UntypedFormControl(this.localCamera.ftp, [Validators.required]),
        retriggerWindow: new UntypedFormControl(this.localCamera.retriggerWindow, [Validators.required])
      })
    }
  }

  ngAfterViewInit(): void {
  }
}
