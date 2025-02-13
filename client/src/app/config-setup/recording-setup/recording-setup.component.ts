import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SharedModule} from "../../shared/shared.module";
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";
import {ReportingComponent} from "../../reporting/reporting.component";
import {Camera, Stream} from "../../cameras/Camera";
import {
  AbstractControl, UntypedFormArray,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";
import {MatSelectChange} from "@angular/material/select";
import {CameraService} from "../../cameras/camera.service";
import {HttpErrorResponse} from "@angular/common/http";
import {BehaviorSubject} from "rxjs";
import {ExcludeOwnStreamPipe} from "../exclude-own-stream.pipe";
import {MatCheckboxChange} from "@angular/material/checkbox";

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
  imports: [SharedModule, SharedAngularMaterialModule, ExcludeOwnStreamPipe]
})
export class RecordingSetupComponent implements OnInit, AfterViewInit {
  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent
  @Input() camera!: Camera | undefined | null;
  @Input() cameras!: Map<string, Camera>;
  @Input() camKey!: string;
  readonly streamColumns = ['threshold', 'trigger_recording_on', 'preambleFrames', 'mask_file', 'video_width', 'video_height'];

  localCamera!: Camera;
  streamControls: UntypedFormArray[] = [];
  formGroup!: UntypedFormGroup;

  constructor(public cameraSvc: CameraService) {
  }

  setRecordingType($event: MatSelectChange) {
    this.localCamera.recordingType = $event.value;
    switch(this.localCamera.recordingType) {
      case 'none':
        this.localCamera.ftp = 'none';
        this.localCamera.streams.forEach((stream) => {
          if(stream && stream.motion) {
            stream.motion.enabled = false;
            stream.motion.trigger_recording_on = '';
          }
        });

        break;
      case 'motion':
        this.localCamera.ftp = 'none';
        let anyEnabled = false;
        let lastKey: string = "";
        this.localCamera.streams.forEach((stream, key) => {
          lastKey = key;
          if(stream.motion.enabled)
            anyEnabled = true;
        });

        const lastStream = this.localCamera.streams.get(lastKey);
        if(!anyEnabled && lastStream)
          lastStream.motion.enabled = true;
        break;
      case 'ftp':
        this.localCamera.ftp = 'none';
        this.localCamera.streams.forEach((stream) => {
          if(stream && stream.motion) {
            stream.motion.enabled = false;
            stream.motion.trigger_recording_on = '';
          }
        });
        break;
      case 'pullpoint':
        this.localCamera.ftp = 'none';
        this.localCamera.streams.forEach((stream) => {
          if(stream && stream.motion) {
            stream.motion.enabled = false;
            stream.motion.trigger_recording_on = '';
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

  getStreamControl(streamIndex: number, fieldName: string): UntypedFormControl {
    return this.streamControls[0].at(streamIndex).get(fieldName) as UntypedFormControl;
  }

  updateStream(streamIndex: number, field: string, value: any) {
    Array.from(  // Streams
        this.localCamera.streams.values()).forEach((stream: Stream, i) => {
      if (i === streamIndex) { // @ts-ignore
        stream[field] = value;
      }
    });
  }

  updateStreamField(streamIndex: number, field: string) {
    const control = this.getStreamControl(streamIndex, field);
    if (control) {
      this.updateStream(streamIndex, field, control.value);
    }
  }

  /**
   * setMotionStatus: Enable/disable motion sensing on the stream according to the checkbox state.
   * @param $event
   * @param stream
   * @param cam
   */
  setMotionStatus($event: MatCheckboxChange, stream: Stream, cam: Camera) {
    if ($event.checked) {
      // Set all to disabled before setting this one as only one is allowed to be selected.
      cam.streams.forEach((stream: Stream) => {
        stream.motion.enabled = false;
        stream.recording.enabled = false;
        stream.motion.trigger_recording_on = '';
      })
    } else {
      stream.recording.enabled = false;
    }

    stream.motion.enabled = $event.checked;
    // Ensure that the trigger_recording_on setting is shown
  }

  setStreamForMotionDetection($event: MatSelectChange) {
    this.localCamera.streams.forEach((stream, key) => {
      stream.motion.enabled = $event.value === key;
      stream.recording.enabled = false;
      stream.motion.trigger_recording_on = '';
    });
    this.setUpFormGroup()
  }

  getStreamKeyForMotionDetection(): string {
    let retVal = 'none';
    this.localCamera.streams.forEach((stream, key) => {
      if (stream.motion.enabled)
        retVal = key;
    });
    return retVal;
  }

  getStreamForMotionDetection(): Stream {
    let retVal = undefined;
    const streamKey = this.getStreamKeyForMotionDetection();
    if (streamKey != 'none')
      retVal = this.localCamera.streams.get(streamKey);
    return retVal as Stream;
  }


  cancel() {
    this.hideDialogue.emit();
  }

  anyInvalid(): boolean {
    return this.localCamera.recordingType === 'ftp' && this.localCamera.ftp === 'none';
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

  uploadMaskFile($event: Event, streamIndex: number) {
    let fileUploadInput: HTMLInputElement = $event.target as HTMLInputElement;
    if (fileUploadInput.files && fileUploadInput.files.length > 0) {
      let stream: Stream = Array.from(this.localCamera.streams.values())[streamIndex];

      stream.motion.mask_file = fileUploadInput?.files[0].name;

      let control: UntypedFormControl = this.getStreamControl(streamIndex, 'mask_file');
      control.setValue(stream.motion.mask_file);
      if (control.valid) {
        // Upload file to server
        this.cameraSvc.uploadMaskFile(fileUploadInput?.files[0])
            .subscribe(() => {
                  this.reporting.successMessage = stream.motion.mask_file + ' uploaded successfully'
                },
                (reason) => {
                  this.reporting.errorMessage = reason
                });
      } else
        this.reporting.errorMessage = new HttpErrorResponse({
          error: "The file " + stream.motion.mask_file + (control.errors?.mask_file ? " is not a valid mask file"
              : control.errors?.duplicate ? " is used with more than one stream"
                  : " has an unspecified error"),
          status: 0,
          statusText: "",
          url: undefined
        });

      // Clear the input so that selecting the same file again still triggers an onchange event
      fileUploadInput.value = '';
    }
  }

  getPreambleFramesDisabledState(cam: Camera, stream: {key: string, value:Stream}): boolean {
    let hasMotionTriggeredRecording: boolean = false;

    cam.streams.forEach((s) => {
        if(s.motion.enabled && s.motion.trigger_recording_on.endsWith(stream.key))
          hasMotionTriggeredRecording = true;
    })
    return !hasMotionTriggeredRecording && cam.ftp!==stream.key;
  }

  setUpFormGroup() {
    if (this.localCamera !== undefined && this.localCamera !== null) {
      let streamList$: BehaviorSubject<Stream[]> = new BehaviorSubject<Stream[]>(Array.from(this.localCamera.streams.values()));

      const toStreamGroups = streamList$.value.map((stream: Stream, key: number) => {
        return new UntypedFormGroup({
          video_width: new UntypedFormControl({
            value: stream.video_width,
            disabled: !stream.motion?.enabled
          }, [Validators.required, Validators.min(90), Validators.max(5000)]),
          video_height: new UntypedFormControl({
            value: stream.video_height,
            disabled: !stream.motion?.enabled
          }, [Validators.required, Validators.min(90), Validators.max(3000)]),
          //  enabled: new FormControl(stream.motion.enabled, [Validators.nullValidator]),
          threshold: new UntypedFormControl({
            value: stream.motion?.threshold != undefined ? stream.motion.threshold : 1500,
            disabled: !stream.motion.enabled
          }, [Validators.required, Validators.min(1), Validators.max(2147483647)]),
          trigger_recording_on: new UntypedFormControl({
            value: stream.motion.trigger_recording_on,
            disabled: !stream.motion.enabled
          }, [Validators.nullValidator]),
          preambleFrames: new UntypedFormControl({
            value: stream.preambleFrames,
            disabled: this.getPreambleFramesDisabledState(this.localCamera!, {key: 'stream'+key, value:stream}),
          }, [Validators.min(0), Validators.max(400)]),
          mask_file: new UntypedFormControl({
            value: stream.motion.mask_file,
            disabled: !stream.motion.enabled
          }, [isValidMaskFileName(this.cameras), Validators.maxLength(55)])
        }, {updateOn: "change"});
      });

      this.streamControls.splice(0, this.streamControls.length);
      this.streamControls.push(new UntypedFormArray(toStreamGroups));

      //    const streamKeyForMotionDetection = this.getStreamForMotionDetectionKey();
      // this.triggerRecordingOn = streamForMotionDetection?.motion?.trigger_recording_on;
      this.formGroup = new UntypedFormGroup({
        recordingType: new UntypedFormControl(this.localCamera.recordingType, [Validators.required]),
        ftpStreamSelect: new UntypedFormControl(this.localCamera.ftp, [Validators.required]),
        streamForMotionDetection: new UntypedFormControl(this.getStreamKeyForMotionDetection(), [Validators.required])
      })
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
