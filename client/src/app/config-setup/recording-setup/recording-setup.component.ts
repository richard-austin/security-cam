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
  @Input() camKey!: string;

  localCamera!: Camera;

  formGroup!: UntypedFormGroup;

  constructor(public cameraSvc: CameraService) {
  }

  setRecordingType($event: MatSelectChange) {
    this.localCamera.recordingType = $event.value;
    switch(this.localCamera.recordingType) {
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
            stream.motion.trigger_recording_on = 'none';
          }
        });
        break;
      case 'pullpoint':
        this.localCamera.ftp = 'none';
        this.localCamera.streams.forEach((stream) => {
          if(stream && stream.motion) {
            stream.motion.enabled = false;
            stream.motion.trigger_recording_on = 'none';
          }
        });
    }
  }

  setRetriggerWindow($event: MatSelectChange) {
    this.localCamera.retriggerWindow = $event.value;
  }

  setSelectedStream($event: MatSelectChange) {
    this.localCamera.ftp = $event.value;
  }

  triggerRecordingOn: string = 'none';
  setStreamForMotionDetection($event: MatSelectChange) {
    this.localCamera.streams.forEach((stream, key) => {
      stream.motion.enabled = $event.value === key;
      this.triggerRecordingOn = stream.motion.trigger_recording_on = 'none';
    });
    this.setUpFormGroup();
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

  getPreambleFrames(): number {
    let preambleFrames = 0;
    let lc = this.localCamera;
    if(lc) {
      if (lc.recordingType === 'motion') {
        preambleFrames = this.getStreamForMotionDetection().preambleFrames;
      } else if (lc.recordingType === 'ftp' || lc.recordingType === 'pullpoint') {
        let ftpStream = lc.streams.get(lc.ftp);
        if (lc.ftp !== 'none' && ftpStream !== undefined)
          preambleFrames = lc.streams.get(lc.ftp)?.preambleFrames as number
      }
    }

    return preambleFrames;
  }

  motionEnabledOnAnyStream(): boolean {
    let retVal = false;
    this.localCamera.streams.forEach((stream) => {
      if (stream?.motion.enabled)
        retVal = true;
    });
    return retVal;
  }

  setThreshold($event: Event, stream: Stream) {
    if (stream.motion.enabled) {
      let input: HTMLInputElement = $event.target as HTMLInputElement;
      stream.motion.threshold = Number(input.value);
    }
  }

  setRecordingTrigger($event: MatSelectChange) {
    this.localCamera.streams.forEach((stream) => {
      if (stream.motion.enabled) {
        this.triggerRecordingOn = stream.motion.trigger_recording_on = $event.value;
      }
    })
  }

  // uploadMaskFile($event: Event, camKey: string, camIndex: number, streamIndex: number) {
  //   let fileUploadInput: HTMLInputElement = $event.target as HTMLInputElement;
  //   if (fileUploadInput.files && fileUploadInput.files.length > 0) {
  //     let stream: Stream | undefined = undefined;
  //
  //     this.localCamera.streams.forEach((s) => {
  //       if (s?.motion.enabled)
  //         stream = s;
  //     });
  //     if (stream !== undefined) {
  //       stream.motion.mask_file = fileUploadInput?.files[0].name;
  //
  //       let control: UntypedFormControl = this.getStreamControl(camIndex, streamIndex, 'mask_file');
  //       control.setValue(stream.motion.mask_file);
  //       if (control.valid) {
  //         // Upload file to server
  //         this.cameraSvc.uploadMaskFile(fileUploadInput?.files[0])
  //             .subscribe(() => {
  //                   this.reporting.successMessage = stream.motion.mask_file + ' uploaded successfully'
  //                 },
  //                 (reason) => {
  //                   this.reporting.errorMessage = reason
  //                 });
  //       } else
  //         this.reporting.errorMessage = new HttpErrorResponse({
  //           error: "The file " + stream.motion.mask_file + (control.errors?.mask_file ? " is not a valid mask file"
  //               : control.errors?.duplicate ? " is used with more than one stream"
  //                   : " has an unspecified error"),
  //           status: 0,
  //           statusText: "",
  //           url: undefined
  //         });
  //
  //       // Clear the input so that selecting the same file again still triggers an onchange event
  //       fileUploadInput.value = '';
  //     }
  //   }
  // }


  // TODO: Add condition for event triggered recordings
  private anyStreamSelected(): boolean {
    let retVal = false;
    if (this.localCamera.recordingType === 'ftp' && this.localCamera.ftp !== 'none')
      retVal = true;
    else if (this.localCamera.recordingType === 'motion')
      this.localCamera.streams.forEach((stream, key) => {
        if (stream.motion.enabled && stream.motion.trigger_recording_on !== '')
          retVal = true;
      });
    return retVal;
  }

  getPreambleFramesDisabledState() {
    let c = this.localCamera;
    return c.recordingType === 'none' || (c.recordingType === 'ftp' && c.ftp === 'none') ||
        (c.recordingType === 'motion' && !this.anyStreamSelected());
  }

  // TODO: Add condition for event triggered recordings
  setPreambleFrames($event: MatSelectChange) {
    this.localCamera.streams.forEach((stream, key) => {
      if (stream.motion.enabled || this.localCamera.ftp == key)
        stream.preambleFrames = $event.value;
    });
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
    // let retVal: Camera = new Camera();
    // Object.assign(retVal, cam);
    // return retVal;
     return structuredClone(cam); // { ... cam };
  }

  setUpFormGroup() {
    if (this.camera !== undefined && this.camera !== null) {
      //    const streamKeyForMotionDetection = this.getStreamForMotionDetectionKey();
         const streamForMotionDetection = this.getStreamForMotionDetection();
         this.triggerRecordingOn = streamForMotionDetection?.motion?.trigger_recording_on;
      this.formGroup = new UntypedFormGroup({
        recordingType: new UntypedFormControl(this.localCamera.recordingType, [Validators.required]),
        ftpStreamSelect: new UntypedFormControl(this.localCamera.ftp, [Validators.required]),
        retriggerWindow: new UntypedFormControl(this.localCamera.retriggerWindow, [Validators.required]),
        streamForMotionDetection: new UntypedFormControl(this.getStreamKeyForMotionDetection(), [Validators.required]),
        threshold: new UntypedFormControl({
          value: this.getStreamForMotionDetection()?.motion?.threshold != undefined ? this.getStreamForMotionDetection()?.motion.threshold : 1500,
          disabled: !this.getStreamForMotionDetection()?.motion.enabled
        }, [Validators.required, Validators.min(1), Validators.max(2147483647)]),
        triggerRecordingOn: new UntypedFormControl(this.triggerRecordingOn, [Validators.required, Validators.nullValidator]),
        preambleFrames: new UntypedFormControl(this.getPreambleFrames(), [Validators.required, Validators.min(0), Validators.max(400)])
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
