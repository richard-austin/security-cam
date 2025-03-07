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
import {ConfigSetupComponent} from "../config-setup.component";

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
    @Input() parent!: ConfigSetupComponent;
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
        this.localCamera.recordingType = $event.value;
        this.localCamera.motion_detection_stream = 'none';
        this.localCamera.recordingStream = 'none';
        this.localCamera.pullPointTopic = 'none';
        this.localCamera.streams.forEach((stream) => {
            stream.motion.trigger_recording_on = 'none';
            stream.motion.mask_file = '';
            stream.motion.enabled = false;
        })
        this.localCamera.recordingStream = 'none';

        switch (this.localCamera.recordingType) {
            case RecordingType.none:
                break;
            case RecordingType.motionService:
                break;
            case RecordingType.ftpTriggered:
                break;
            case RecordingType.pullPointEventTriggered:
        }
        this.setUpFormGroup();
    }

    setSelectedStream($event: MatSelectChange) {
        this.localCamera.recordingStream = $event.value;
        this.setUpFormGroup();
    }


    setSelectedPullPointRecordingTrigger($event: MatSelectChange) {
        this.localCamera.pullPointTopic = $event.value;
    }

    getControl(fieldName: string): UntypedFormControl {
        return this.formGroup.get(fieldName) as UntypedFormControl;
    }

    updateRecordingTrigger() {
        const control = this.getControl('trigger_recording_on');
        if (control) {
            this.localCamera.streams.forEach((stream, key) => {
                stream.motion.trigger_recording_on = this.localCamera.motion_detection_stream === key ? control.value : 'none';
            });
            this.setUpFormGroup();
        }
    }

    updateVideoWidth() {
        const control = this.getControl('video_width');
        if (control) {
            const stream = this.localCamera.streams.get(this.localCamera.motion_detection_stream);
            if (stream !== undefined) {
                stream.video_width = control.value;
            }
        }
    }

    updateVideoHeight() {
        const control = this.getControl('video_height');
        if (control) {
            const stream = this.localCamera.streams.get(this.localCamera.motion_detection_stream);
            if (stream !== undefined) {
                stream.video_height = control.value;
            }
        }
    }

    updateThreshold() {
        const control = this.getControl('threshold');
        if (control) {
            const stream = this.localCamera.streams.get(this.localCamera.motion_detection_stream);
            if (stream !== undefined) {
                stream.motion.threshold = control.value;
            }
        }
    }


    getTriggeredStream(): Stream | undefined {
        const cam = this.localCamera;
        let retVal: Stream | undefined;
        switch (cam.recordingType) {
            case RecordingType.motionService:
                let s = cam.streams.get(cam.motion_detection_stream);
                if (s) {
                    retVal = cam.streams.get(s.motion.trigger_recording_on)
                }
                break;

            case RecordingType.ftpTriggered:
            case RecordingType.pullPointEventTriggered:
                retVal = cam.streams.get(cam.recordingStream);
                break;
        }
        return retVal;
    }

    updatePreambleFrames() {
        const control = this.getControl('preambleFrames');
        const stream = this.getTriggeredStream();
        if (stream !== undefined) {
            stream.preambleFrames = control.value;
        }
    }

    setRetriggerWindow() {
        const control = this.getControl('retriggerWindow');
        if (control)
            this.localCamera.retriggerWindow = control.value;
    }

    setStreamForMotionDetection($event: MatSelectChange) {
        this.localCamera.motion_detection_stream = $event.value;
        this.localCamera.streams.forEach((stream, key) => {
            stream.motion.enabled = $event.value === key;
        });

        this.setUpFormGroup();
    }

    cancel() {
        this.ngOnInit();
        this.hideDialogue.emit();
    }

    anyInvalid(): boolean {
        const cam = this.localCamera;
        let motionStreamSelected = false;
        let hasFTPRecordingStream = cam.recordingStream !== 'none';

        cam.streams.forEach((stream, key) => {
            if (stream.motion.enabled)
                motionStreamSelected = true;
        });
        return !this.formGroup.valid ||
            cam.recordingType === RecordingType.motionService && !motionStreamSelected ||
            cam.recordingType === RecordingType.ftpTriggered && !hasFTPRecordingStream;
    }

    confirmChanges() {
        if (this.camera !== undefined && this.camera !== null) {
            const cam = this.localCamera;

            // The values are copied over field by field rather than by using Object.assign on the whole camera objects
            //  as doing it that way would overwrite previously edited fields which are not part of the recording set up
            this.camera.recordingType = cam.recordingType;
            this.camera.motion_detection_stream = cam.motion_detection_stream;
            this.camera.recordingStream = cam.recordingStream;
            this.camera.retriggerWindow = cam.retriggerWindow;
            this.camera.pullPointTopic = cam.pullPointTopic;

            cam.streams.forEach((stream, key) => {
                if (this.camera) {
                    const targetStream = this.camera.streams.get(key);
                    if (targetStream) {
                        Object.assign(targetStream.recording, stream.recording);
                        Object.assign(targetStream.motion, stream.motion);
                        targetStream.video_height = stream.video_height;
                        targetStream.video_width = stream.video_width;
                        targetStream.preambleFrames = stream.preambleFrames;
                        targetStream.rec_num = stream.rec_num;  // Don't really need to do this one as it's set up in FixUpCameras.
                    }
                }
            });

            this.hideDialogue.emit();
        }
    }

    clone(cam: Camera): Camera {
        return structuredClone(cam);
    }

    uploadMaskFile($event: Event) {
        let fileUploadInput: HTMLInputElement = $event.target as HTMLInputElement;
        if (fileUploadInput.files && fileUploadInput.files.length > 0) {
            let control: AbstractControl | null = this.formGroup.get('mask_file');
            if (control) {
                const stream = this.localCamera.streams.get(this.localCamera.motion_detection_stream);
                if (stream !== undefined) {
                    stream.motion.mask_file = fileUploadInput?.files[0].name;
                    if (!this.checkMaskFileReuse()) {
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
                    } else
                        stream.motion.mask_file = '';
                }
            }

            // Clear the input so that selecting the same file again still triggers an onchange event
            fileUploadInput.value = '';
        }
    }

    clearMaskFile() {
        const stream = this.localCamera.streams.get(this.localCamera.motion_detection_stream);
        if (stream)
            stream.motion.mask_file = ''
    }

    getPreambleFramesDisabledState(): boolean {
        const cam = this.localCamera;
        const motionDetectStream = cam.streams.get(cam.motion_detection_stream);
        let disabled: boolean = true;
        switch (cam.recordingType) {
            case RecordingType.none:
                break;
            case RecordingType.motionService:
                if (motionDetectStream !== undefined && motionDetectStream.motion.trigger_recording_on !== 'none')
                    disabled = false;
                break;
            case RecordingType.ftpTriggered:
                if (cam.recordingStream !== 'none')
                    disabled = false;
                break;
            case RecordingType.pullPointEventTriggered:
                if(cam.pullPointTopic !== 'none' && cam.recordingStream !== 'none')
                    disabled = false;
                break;
        }
        return disabled;
    }

    /**
     * getMaskFileName: This is called from the parent component when checking for multiple use of mask file names
     */
    getMaskFileName(): string | undefined {
        let retVal;
        const cam = this.localCamera;

        if (cam.recordingType === RecordingType.motionService) {
            const stream = cam.streams.get(cam.motion_detection_stream);
            if (stream && stream.motion.mask_file && stream.motion.mask_file !== "")
                retVal = stream.motion.mask_file
        }
        return retVal;
    }

    checkMaskFileReuse() {
        return this.parent.checkForMaskFileReUse();
    }

    setupData(): void {
        if (this.camera)
            this.localCamera = this.clone(this.camera);
        this.setUpFormGroup();
    }

    readonly streamsOrNoneRegex = /^(none|stream[1-9]{1,2}?)$/;

    setUpFormGroup() {
        if (this.localCamera !== undefined && this.localCamera !== null) {
            const cam = this.localCamera;
            const motionDetectStream = cam.streams.get(cam.motion_detection_stream);
            const zeroValue = 0;
            const triggeredStream = this.getTriggeredStream();
            this.formGroup = new UntypedFormGroup({
                video_width: new UntypedFormControl({
                    value: motionDetectStream !== undefined ? motionDetectStream.video_width : zeroValue,
                    disabled: cam.motion_detection_stream === 'none'
                }, [Validators.required, Validators.min(90), Validators.max(5000)]),
                video_height: new UntypedFormControl({
                    value: motionDetectStream !== undefined ? motionDetectStream.video_height : zeroValue,
                    disabled: cam.motion_detection_stream === 'none'
                }, [Validators.required, Validators.min(90), Validators.max(3000)]),
                //  enabled: new FormControl(stream.motion.enabled, [Validators.nullValidator]),
                threshold: new UntypedFormControl({
                    value: motionDetectStream !== undefined ? motionDetectStream.motion.threshold : 1500,
                    disabled: cam.motion_detection_stream === 'none'
                }, [Validators.required, Validators.min(1), Validators.max(2147483647)]),
                trigger_recording_on: new UntypedFormControl({
                    value: motionDetectStream !== undefined ? motionDetectStream.motion.trigger_recording_on : 'none',
                    disabled: cam.motion_detection_stream === 'none'
                }, [Validators.pattern(this.streamsOrNoneRegex)]),
                retriggerWindow: new UntypedFormControl({
                    value: cam.retriggerWindow,
                    disabled: cam.recordingStream === 'none',
                }, [Validators.pattern(/^(10|20|30|40|50|60|70|80|90|100)$/)]),
                recordingType: new UntypedFormControl(cam.recordingType, [Validators.required]),
                recordingStreamSelect: new UntypedFormControl(cam.recordingStream, [Validators.required, Validators.pattern(this.streamsOrNoneRegex)]),
                streamForMotionDetection: new UntypedFormControl(cam.motion_detection_stream, [Validators.required, Validators.pattern(this.streamsOrNoneRegex)]),
                preambleFrames: new UntypedFormControl({
                    value: triggeredStream ? triggeredStream.preambleFrames : 0,
                    disabled: this.getPreambleFramesDisabledState(),
                }, [Validators.min(0), Validators.max(400)]),
                mask_file: new UntypedFormControl({
                    value: motionDetectStream !== undefined ? motionDetectStream.motion.mask_file : '',
                    disabled: cam.motion_detection_stream === 'none'
                }, [isValidMaskFileName(this.cameras), Validators.maxLength(55)]),
                pullPointRecordingTrigger: new UntypedFormControl({
                    value: cam.pullPointTopic,
                    disabled: cam.recordingType !== RecordingType.pullPointEventTriggered || cam.pullPointEvents.length <= 0
                }, [])
            }, {updateOn: "change"});
        }
    }

    ngOnInit(): void {
        this.setupData();
    }

    ngAfterViewInit(): void {
    }
}
