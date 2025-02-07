import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SharedModule} from "../../shared/shared.module";
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";
import {ReportingComponent} from "../../reporting/reporting.component";
import {Camera} from "../../cameras/Camera";
import {UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {MatSelectChange} from "@angular/material/select";
import {CameraService} from "../../cameras/camera.service";

@Component({
  selector: 'app-recording-setup',
  templateUrl: './recording-setup.component.html',
  styleUrl: './recording-setup.component.scss',
  imports:[SharedModule, SharedAngularMaterialModule]
})
export class RecordingSetupComponent implements OnInit, AfterViewInit {
  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent
  @Input() camera!: Camera | undefined | null;
  recordingType: string = 'none';
  retriggerWindow: number = 10;
  selectedStream: string | boolean = "none";

  formGroup!: UntypedFormGroup;

  constructor(public cameraSvc: CameraService) {
  }

  setRecordingType($event: MatSelectChange) {
    this.recordingType = $event.value;
  }

  setRetriggerWindow($event: MatSelectChange) {
    this.retriggerWindow = $event.value;
  }

  setSelectedStream($event: MatSelectChange) {
    this.selectedStream = $event.value;
  }

  cancel() {
    this.hideDialogue.emit();
  }

  anyInvalid(): boolean {
    return this.recordingType === 'ftp' && this.selectedStream === 'none';
  }

  confirmChanges() {
    if (this.camera !== undefined && this.camera !== null) {
      this.camera.recordingType = this.recordingType;
      this.camera.retriggerWindow = this.retriggerWindow;
      this.camera.ftp = this.selectedStream;
      this.hideDialogue.emit();
    }
  }

  ngOnInit(): void {
    if (this.camera !== undefined && this.camera !== null) {
      this.recordingType = this.camera.recordingType;
      this.retriggerWindow = this.camera.retriggerWindow;
      this.selectedStream = this.camera.ftp;

      this.formGroup = new UntypedFormGroup({
        recordingType: new UntypedFormControl(this.recordingType, [Validators.required]),
        ftpStreamSelect: new UntypedFormControl(this.selectedStream, [Validators.required]),
        retriggerWindow: new UntypedFormControl(this.retriggerWindow, [Validators.required])
      })
    }
  }

  ngAfterViewInit(): void {
  }
}
