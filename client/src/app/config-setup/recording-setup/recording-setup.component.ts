import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SharedModule} from "../../shared/shared.module";
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";
import {ReportingComponent} from "../../reporting/reporting.component";
import {Camera, RecordingType, Stream} from "../../cameras/Camera";
import {
  AbstractControl,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";
import {MatSelectChange} from "@angular/material/select";
import {CameraService} from "../../cameras/camera.service";
import {HttpErrorResponse} from "@angular/common/http";

export function isValidMaskFileName(cameras: Map<string, Camera>): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {

    const value = control.value;

    if (!value) {
      return null;
    }

    let allFiles: Set<string> = new Set<string>();
    let duplicateMaskFile: boolean = false;

    const fileNameValid = RegExp('^[a-zA-Z0-9-_]+.pgm$').test(value);
    // Check that no file name is being used by more than one camera
    cameras.forEach((cam: Camera) => {
      cam.streams.forEach((stream: Stream) => {
        if (stream.motion.enabled && stream.motion.mask_file !== '') {
          if (allFiles.has(stream.motion.mask_file))
            duplicateMaskFile = true;
          else
            allFiles.add(stream.motion.mask_file);
        }
      })
    })
    return !fileNameValid || duplicateMaskFile ? {mask_file: !fileNameValid, duplicate: duplicateMaskFile} : null;
  }
}

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
  @Input() cameras!: Map<string, Camera>;
  @Input() camKey!: string;

  localCamera!: Camera;
  formGroup!: UntypedFormGroup;
  protected readonly RecordingType = RecordingType;

  constructor(public cameraSvc: CameraService) {
  }

  setRecordingType($event: MatSelectChange) {
    this.localCamera.recording.recordingType = $event.value;
    this.localCamera.motion.motion_detection_stream = 'none';
    this.localCamera.motion.trigger_recording_on = 'none';
    this.localCamera.ftp = 'none';

    switch (this.localCamera.recording.recordingType) {
      case RecordingType.none:
        break;
      case RecordingType.motionService:
        break;
      case RecordingType.ftpTriggered:
        this.localCamera.ftp = 'none';
        this.localCamera.streams.forEach((stream) => {
          if(stream && stream.motion) {
            stream.motion.enabled = false;
            stream.motion.trigger_recording_on = 'none';
          }
        });
        break;
      case RecordingType.pullPointEventTriggered:
        this.localCamera.ftp = 'none';
        this.localCamera.streams.forEach((stream) => {
          if(stream && stream.motion) {
            stream.motion.enabled = false;
            stream.motion.trigger_recording_on = 'none';
          }
        });
    }
    this.setUpFormGroup();
  }

  ftpSet(cam: Camera): boolean {
    return cam.ftp !== 'none' && typeof cam.ftp !== 'boolean';
  }

  setSelectedStream($event: MatSelectChange) {
    this.localCamera.ftp = $event.value;
  }

  getControl(fieldName: string): UntypedFormControl {
    return this.formGroup.get(fieldName) as UntypedFormControl;
  }

  updateRecordingTrigger($event: MatSelectChange) {
    const control = this.getControl('trigger_recording_on');
    if (control) {
      const stream = this.localCamera.streams.get(this.localCamera.motion.motion_detection_stream);
      if(stream !== undefined) {
        stream.motion.trigger_recording_on = control.value;
      }
    }
  }

  updateVideoWidth() {
    const control = this.getControl('video_width');
    if (control) {
      const stream = this.localCamera.streams.get(this.localCamera.motion.motion_detection_stream);
      if(stream !== undefined) {
        stream.video_width = control.value;
      }
     }
  }

  updateVideoHeight() {
    const control = this.getControl('video_height');
    if (control) {
      const stream = this.localCamera.streams.get(this.localCamera.motion.motion_detection_stream);
      if(stream !== undefined) {
        stream.video_height = control.value;
      }
    }
  }

  updateThreshold() {
    const control = this.getControl('threshold');
    if (control) {
      const stream = this.localCamera.streams.get(this.localCamera.motion.motion_detection_stream);
      if(stream !== undefined) {
        stream.motion.threshold = control.value;
      }
    }
  }

  updatePreambleFrames() {
    const control = this.getControl('preambleFrames');
    if (control) {
      const stream = this.localCamera.streams.get(this.localCamera.motion.motion_detection_stream);
      if(stream !== undefined) {
        stream.preambleFrames = control.value;
      }
    }
  }

  setStreamForMotionDetection($event: MatSelectChange) {
    this.localCamera.motion.motion_detection_stream = $event.value;
    this.setUpFormGroup();
  }

  cancel() {
    this.ngOnInit();
    this.hideDialogue.emit();
  }

  anyInvalid(): boolean {
    return !this.formGroup.valid;
  }

  confirmChanges() {
    if (this.camera !== undefined && this.camera !== null) {
      Object.assign(this.camera, this.localCamera);
      this.hideDialogue.emit();
    }
  }

  clone(cam: Camera) : Camera {
    return structuredClone(cam);
  }

  uploadMaskFile($event: Event) {
    const cam = this.localCamera;
    let fileUploadInput: HTMLInputElement = $event.target as HTMLInputElement;
    if (fileUploadInput.files && fileUploadInput.files.length > 0) {
      let control: AbstractControl | null = this.formGroup.get('mask_file');
      if (control) {
        const stream = this.localCamera.streams.get(this.localCamera.motion.motion_detection_stream);
        if(stream !== undefined) {
          stream.motion.mask_file = fileUploadInput?.files[0].name;
          control.setValue(stream.motion.mask_file);
        }
        if (control.valid) {
          // Upload file to server
          this.cameraSvc.uploadMaskFile(fileUploadInput?.files[0])
              .subscribe(() => {
                    this.reporting.successMessage = cam.motion.mask_file + ' uploaded successfully'
                  },
                  (reason) => {
                    this.reporting.errorMessage = reason
                  });
        } else
          this.reporting.errorMessage = new HttpErrorResponse({
            error: "The file " + cam.motion.mask_file + (control.errors?.mask_file ? " is not a valid mask file"
                : control.errors?.duplicate ? " is used with more than one stream"
                    : " has an unspecified error"),
            status: 0,
            statusText: "",
            url: undefined
          });
      }

      // Clear the input so that selecting the same file again still triggers an onchange event
      fileUploadInput.value = '';
    }
  }

  getPreambleFramesDisabledState(): boolean {
    const cam = this.localCamera;
    const motionDetectStream = cam.streams.get(cam.motion.motion_detection_stream);
    let disabled: boolean = true;
    switch(cam.recording.recordingType) {
      case RecordingType.none:
        break;
      case RecordingType.motionService:
        if(motionDetectStream !== undefined && motionDetectStream.motion.trigger_recording_on !== 'none')
          disabled = false;
        break;
      case RecordingType.ftpTriggered:
        if(cam.ftp !== 'none')
          disabled = false;
        break;
    }
    return disabled;
  }

  setUpFormGroup() {
    if (this.localCamera !== undefined && this.localCamera !== null) {

      const motionDetectStream = this.localCamera.streams.get(this.localCamera.motion.motion_detection_stream);
      const zeroValue = 0;

      this.formGroup = new UntypedFormGroup({
        video_width: new UntypedFormControl({
          value: motionDetectStream !== undefined ? motionDetectStream.video_width : zeroValue,
          disabled: this.localCamera.motion.motion_detection_stream === 'none'
        }, [Validators.required, Validators.min(90), Validators.max(5000)]),
        video_height: new UntypedFormControl({
          value:  motionDetectStream !== undefined ? motionDetectStream.video_height : zeroValue,
          disabled: this.localCamera.motion.motion_detection_stream === 'none'
        }, [Validators.required, Validators.min(90), Validators.max(3000)]),
        //  enabled: new FormControl(stream.motion.enabled, [Validators.nullValidator]),
        threshold: new UntypedFormControl({
          value: motionDetectStream !== undefined ? motionDetectStream.motion.threshold : 1500,
          disabled: this.localCamera.motion.motion_detection_stream === 'none'
        }, [Validators.required, Validators.min(1), Validators.max(2147483647)]),
        trigger_recording_on: new UntypedFormControl({
          value: motionDetectStream !== undefined ? motionDetectStream.motion.trigger_recording_on : 'none',
          disabled: this.localCamera.motion.motion_detection_stream === 'none'
        }, [Validators.nullValidator]),

        recordingType: new UntypedFormControl(this.localCamera.recording.recordingType, [Validators.required]),
        ftpStreamSelect: new UntypedFormControl(this.localCamera.ftp, [Validators.required]),
        streamForMotionDetection: new UntypedFormControl(this.localCamera.motion.motion_detection_stream, [Validators.required]),
        preambleFrames: new UntypedFormControl({
          value: motionDetectStream !== undefined ? motionDetectStream.preambleFrames : 0,
          disabled: this.getPreambleFramesDisabledState(),
        }, [Validators.min(0), Validators.max(400)]),
          mask_file: new UntypedFormControl({
            value: motionDetectStream !== undefined ? motionDetectStream.motion.mask_file : '',
            disabled: this.localCamera.motion.motion_detection_stream === 'none'
          }, [isValidMaskFileName(this.cameras), Validators.maxLength(55)])
      }, {updateOn: "change"});
    }
  }

  ngOnInit(): void {
    if (this.camera)
      this.localCamera = this.clone(this.camera);
    this.setUpFormGroup();
  }

  ngAfterViewInit(): void {
  }
}
