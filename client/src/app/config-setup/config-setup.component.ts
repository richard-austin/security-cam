import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef, HostListener,
    isDevMode,
    OnDestroy,
    OnInit, QueryList,
    ViewChild, ViewChildren,
} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, CameraParamSpec, Stream} from "../cameras/Camera";
import {ReportingComponent} from '../reporting/reporting.component';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {
    AbstractControl,
    UntypedFormArray,
    UntypedFormControl,
    UntypedFormGroup,
    ValidationErrors,
    ValidatorFn,
    Validators
} from "@angular/forms";
import {MatCheckboxChange} from '@angular/material/checkbox';
import {MatSelectChange} from '@angular/material/select';
import {BehaviorSubject} from 'rxjs';
import {HttpErrorResponse} from "@angular/common/http";
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {KeyValue, KeyValuePipe} from '@angular/common';
import {UtilsService} from '../shared/utils.service';
import {OnvifCredentialsComponent} from "./camera-credentials/onvif-credentials.component";
import {OnvifFailuresComponent} from "./onvif-failures/onvif-failures.component";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {AddAsOnvifDeviceComponent} from "./add-as-onvif-device/add-as-onvif-device.component";
import {SharedModule} from "../shared/shared.module";
import {RecordingSetupComponent} from "./recording-setup/recording-setup.component";
import {CanComponentDeactivate, CanDeactivateType} from "../guards/can-deactivate.guard";
import {ConfirmCanDeactivateComponent} from "./confirm-can-deactivate/confirm-can-deactivate.component";
import {RowDeleteConfirmComponent} from "./row-delete-confirm/row-delete-confirm.component";

declare let objectHash: (obj: Object) => string;

export function validateTrueOrFalse(fieldCondition: {}): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        let invalidValue: boolean = control.value != true && control.value !== false;
        return invalidValue ? fieldCondition : null;
    }
}

@Component({
    selector: 'app-config-setup',
    templateUrl: './config-setup.component.html',
    styleUrls: ['./config-setup.component.scss'],
    animations: [
        trigger('detailExpand', [
            state('collapsed', style({height: '0px', minHeight: '0'})),
            state('expanded', style({height: '*'})),
            transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
        ]),
        trigger('openClose', [
            // ...
            state('open', style({
                transform: 'rotate(90deg)'
            })),
            state('closed', style({
                transform: 'rotate(0deg)'
            })),
            transition('open => closed', [
                animate('.2s')
            ]),
            transition('closed => open', [
                animate('.2s')
            ]),
        ])
    ],
    imports: [
        SharedModule,
        SharedAngularMaterialModule,
        OnvifCredentialsComponent,
        OnvifFailuresComponent,
        AddAsOnvifDeviceComponent,
        KeyValuePipe,
        RecordingSetupComponent,
        ConfirmCanDeactivateComponent,
        RowDeleteConfirmComponent,
    ],
    schemas: [],
})
export class ConfigSetupComponent implements CanComponentDeactivate, OnInit, AfterViewInit, OnDestroy {
    @ViewChild('errorReporting') reporting!: ReportingComponent;
    @ViewChild('outputframeid') snapshotImage!: ElementRef<HTMLImageElement>
    @ViewChild('scrollable_content') scrollableContent!: ElementRef<HTMLElement> | null
    @ViewChildren(RecordingSetupComponent) recordingSetupComponents!: QueryList<RecordingSetupComponent>

    @HostListener('window:beforeunload', ['$event'])
    unloadNotification($event: BeforeUnloadEvent) {
        if (this.dataHasChanged() || this.anyInvalid()) {
            $event.preventDefault();
        }
    }

    @HostListener('window:unload', ['$event'])
        beforeunload($event: any) {
    }

    downloading: boolean = true;
    updating: boolean = false;
    discovering: boolean = false;
    cameras: Map<string, Camera> = new Map<string, Camera>();
    cameraColumns = ['sorting', 'camera_id', 'creds', 'delete', 'expand', 'name', 'cameraParamSpecs', 'recording', 'address', 'snapshotUri', 'useRtspAuth', 'rtspTransport', 'backchannelAudioSupported', 'ptzControls', 'onvifHost'];
    cameraFooterColumns = ['buttons'];

    expandedElement!: Camera | null;
    streamColumns = ['stream_id', 'delete', 'descr', 'audio', 'audio_encoding', 'netcam_uri', 'defaultOnMultiDisplay'];
    streamFooterColumns = ['buttons']
    camControls!: UntypedFormArray;
    streamControls: UntypedFormArray[] = [];
    list$!: BehaviorSubject<Camera[]>;
    confirmSave: boolean = false;
    confirmRestore: boolean = false;
    confirmNew: boolean = false;
    confirmNewLookup: boolean = false;
    snapshotLoading: boolean = false;
    snapshot: SafeResourceUrl | String = '';
    snapShotKey: string = '';
    camForCredentialsEntry: string = "";
    camForRecordingSetup: string = "";
    showAddCameraDialogue: boolean = false;
    isGuest: boolean = true;
    gettingCameraDetails: boolean = false;
    savedDataHash: string = "";
    haveOnvifCredentials: boolean = false;
    showOnvifCredentialsForm: boolean = false;

    failed: Map<string, string> = new Map<string, string>();
    checkDeactivate: boolean = false;
    showCameraDeleteConfirm: string = '';
    showStreamDeleteConfirm: string = '';

    constructor(public cameraSvc: CameraService, public utils: UtilsService, private sanitizer: DomSanitizer, private cd: ChangeDetectorRef) {
    }

    getCamControl(index: number, fieldName: string): UntypedFormControl {
        return this.camControls.at(index).get(fieldName) as UntypedFormControl;
    }

    setPTZControlsCheckboxDisabledState(index: number): boolean {
        let ptzc: UntypedFormControl = this.getCamControl(index, 'ptzControls');

        let ovhc: UntypedFormControl = this.getCamControl(index, 'onvifHost')
        if (ovhc.value == '')
            ptzc.setValue(false);  // Ensure PTZ is set to "off" if onvifHost has the (valid) value empty
        return ovhc.value == '' || !ovhc.valid;
    }

    updateCam(index: number, field: string, value: any) {
        Array.from(this.cameras.values()).forEach((cam: Camera, i) => {
            if (i === index) { // @ts-ignore
                cam[field] = value;
            }
        });
    }

    updateCamField(index: number, field: string) {
        const control = this.getCamControl(index, field);
        if (control) {
            this.updateCam(index, field, control.value);
        }
    }

    getStreamControl(camIndex: number, streamIndex: number, fieldName: string): UntypedFormControl {
        return this.streamControls[camIndex].at(streamIndex).get(fieldName) as UntypedFormControl;
    }

    updateStream(camIndex: number, streamIndex: number, field: string, value: any) {
        Array.from(  // Streams
            Array.from( // Cameras
                this.cameras.values())[camIndex].streams.values()).forEach((stream: Stream, i) => {
            if (i === streamIndex) { // @ts-ignore
                stream[field] = value;
            }
        });
    }

    updateStreamField(camIndex: number, streamIndex: number, field: string) {
        const control = this.getStreamControl(camIndex, streamIndex, field);
        if (control) {
            this.updateStream(camIndex, streamIndex, field, control.value);
        }
    }

    updateAudioEncoding($event: MatSelectChange, stream: Stream) {
        stream.audio_encoding = $event.value;
        stream.audio = $event.value !== "None";

    }

    /**
     *
     * @param camera
     * Get the actual CameraParamSpec object used for the control rather than the copy of it returned from the API
     * call. If we don't do this, the selector won't show the correct setting.
     */
    getCameraParamSpecsReferenceCopy(camera: Camera): CameraParamSpec {
        if (camera?.cameraParamSpecs?.camType === undefined)
            return this.cameraSvc.cameraParamSpecs[0];  // Return the Not Listed option
        else
            return this.cameraSvc.cameraParamSpecs.find((spec) => camera.cameraParamSpecs.camType === spec.camType) as CameraParamSpec;
    }

    getCameraAddressDisabledState(camera: Camera): boolean {
        return false; // Never disabled now
        // if (camera?.cameraParamSpecs?.camType === undefined)
        //   return true;
        // else
        //   return camera.cameraParamSpecs.camType !== cameraType.sv3c && camera.cameraParamSpecs.camType !== cameraType.zxtechMCW5B10X;
    }

    getCameraDeleteDisabledState(cam:{key: string, value: Camera}): boolean {
        return this.cameras.size <= 1 || this.confirmSave || this.confirmNew || this.camForRecordingSetup === cam.key;
    }

    getStreamDeleteDisabledState(cam: { value: Camera; key: string; }, streamKey: string): boolean {
        const camera: Camera = cam.value;
        if(camera !== undefined) {
            const stream = camera.streams.get(streamKey);
            let motionTriggeredRecording = false;

            camera.streams.forEach((stream) => {
                if (stream.motion.trigger_recording_on === streamKey)
                    motionTriggeredRecording = true;
            })
            const isInvolvedWithRecording = (camera.recordingStream === streamKey) ||
                ((stream !== undefined) ? stream!.motion.enabled : false);
            return (camera.streams.size <= 1) || this.confirmSave ||
                this.confirmNew ||
                isInvolvedWithRecording ||
                motionTriggeredRecording ||
                this.camForRecordingSetup === cam.key;
        }
        return false;
    }

    checkForMaskFileReUse(): boolean {
        const maskFiles: Set<string> = new Set<string>();
        let retVal = false;
        this.recordingSetupComponents.forEach((r) => {
            const maskFileName = r.getMaskFileName();
            if (maskFileName) {
                if (maskFiles.has(maskFileName)) {
                    this.reporting.errorMessage = new HttpErrorResponse({error: "Mask file " + maskFileName + " is already in use"});
                    retVal = true;
                } else
                    maskFiles.add(maskFileName)
            }
        });
        return retVal;
    }

    /**
     * setUpTableFormControls: Associate a FormControl with each editable field on the table
     */
    setUpTableFormControls(): void {
        this.streamControls = [];
        this.list$ = new BehaviorSubject<Camera[]>(Array.from(this.cameras.values()));
        const toCameraGroups = this.list$.value.map(camera => {
            let streamList$: BehaviorSubject<Stream[]> = new BehaviorSubject<Stream[]>(Array.from(camera.streams.values()));

            const toStreamGroups = streamList$.value.map((stream: Stream) => {
                return new UntypedFormGroup({
                    descr: new UntypedFormControl({
                        value: stream.descr,
                        disabled: false
                    }, [Validators.required, Validators.maxLength(20), Validators.pattern(/^[a-zA-Z0-9\\ ]{2,20}$/)]),
                    audio: new UntypedFormControl(stream.audio, [Validators.required]),
                    audio_encoding: new UntypedFormControl(stream.audio_encoding, [Validators.required, Validators.pattern(/^(AAC|G711|G726|None|Not Listed)$/)]),
                    netcam_uri: new UntypedFormControl(stream.netcam_uri, [Validators.required, Validators.pattern(/\b((rtsp):\/\/[-\w]+(\.\w[-\w]*)+|(?:[a-z0-9](?:[-a-z0-9]*[a-z0-9])?\.)+(?: com\b|edu\b|biz\b|gov\b|in(?:t|fo)\b|mil\b|net\b|org\b|[a-z][a-z]\b))(\\:\d+)?(\/[^.!,?;"'<>()\[\]{}\s\x7F-\xFF]*(?:[.!,?]+[^.!,?;"'<>()\[\]{}\s\x7F-\xFF]+)*)?/)]),
                }, {updateOn: "change"});
            });

            this.streamControls.push(new UntypedFormArray(toStreamGroups));
            return new UntypedFormGroup({
                name: new UntypedFormControl(camera.name, [Validators.required, Validators.maxLength(25)]),
                address: new UntypedFormControl({
                    value: camera.address,
                    disabled: this.getCameraAddressDisabledState(camera)
                }, [Validators.pattern(/\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}\b/)]),
                cameraParamSpecs: new UntypedFormControl({
                    value: this.getCameraParamSpecsReferenceCopy(camera),
                    disabled: false
                }, [Validators.maxLength(55)]),
                snapshotUri: new UntypedFormControl({
                    value: camera.snapshotUri,
                    disabled: false
                }, [Validators.maxLength(150)]),
                ptzControls: new UntypedFormControl({
                    value: camera.ptzControls,
                    disabled: false
                }, [validateTrueOrFalse({ptzControls: true})]),
                onvifHost: new UntypedFormControl({
                    value: camera.onvifHost,
                    disabled: false,
                }, [Validators.maxLength(22),
                    Validators.pattern(/^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))($|:([0-9]{1,4}|6[0-5][0-5][0-3][0-5])$)/)]),
                useRtspAuth: new UntypedFormControl({
                    value: camera.useRtspAuth == undefined ? false : camera.useRtspAuth,
                    disabled: false,
                }, [validateTrueOrFalse({useRtspAuth: true})]),
                rtspTransport: new UntypedFormControl({
                    value: camera.rtspTransport,
                    disabled: false,
                }, [Validators.required, Validators.pattern(/^(udp|tcp)$/)])
            }, {updateOn: "change"});
        });
        this.camControls = new UntypedFormArray(toCameraGroups);

        // Ensure camera form controls highlight immediately if invalid
        for (let i = 0; i < this.camControls.length; ++i) {
            this.camControls.at(i).markAllAsTouched();
        }

        // Ensure stream form controls highlight immediately if invalid
        this.streamControls.forEach((fa: UntypedFormArray) => {
            for (let i = 0; i < fa.length; ++i) {
                fa.at(i).markAllAsTouched();
            }
        })
    }

    /**
     * deleteCamera: Delete a camera from the cameras map
     * @param key
     */
    deleteCamera(key: string): boolean {
        let retVal: boolean = Array.from(this.cameras.keys()).find(k => k === key) !== undefined;
        this.cameras.delete(key);
        this.FixUpCamerasData();
        return retVal;
    }

    /**
     * deleteStream: Delete a stream from the streams map
     * @param c
     */
    deleteStream(c:{cam: string, stream: string}): boolean {
        let retVal: boolean = false;

        let cam: Camera = this.cameras.get(c.cam) as Camera;
        if (cam !== undefined) {
            retVal = Array.from(cam.streams.keys()).find(k => k === c.stream) !== undefined;
            if (retVal) {
                let enableFirstAsMultiDisplayDefault: boolean = (cam.streams.get(c.stream) as Stream).defaultOnMultiDisplay;
                cam.streams.delete(c.stream);
                cam.streams.forEach((stream) => {
                    // Ensure we don't land up with none selected as default stream to show on multi cameras display
                    if (enableFirstAsMultiDisplayDefault) {
                        stream.defaultOnMultiDisplay = enableFirstAsMultiDisplayDefault;
                        enableFirstAsMultiDisplayDefault = false;
                    }
                    if (stream.motion.enabled)
                        stream.motion.trigger_recording_on = 'none';  // Set all recording triggers to 'None' as the stream keys may be renumbered
                })
            }
            this.FixUpCamerasData();
        }
        return retVal;
    }

    /**
     * FixUpCamerasData: Fix the key names in the cameras and streams maps, so they follow the sequence
     *                   camera1, camera2 or stream1, stream 2 etc. This is run after deleting an item
     *                   from the map. Also number the live streams and recording uri's logically
     */
    FixUpCamerasData(): void {
        let camNum: number = 1;
        let streamNum: number = 1;  // Stream number on camera
        let recNo: number = 1;  // Recording number to be set in the stream object
        let retVal: Map<string, Camera> = new Map<string, Camera>();

        this.cameras.forEach((camera: Camera) => {
            let streamMap: Map<string, Stream> = new Map<string, Stream>();
            if (camera.cameraParamSpecs === undefined || camera.cameraParamSpecs === null)
                camera.cameraParamSpecs = this.cameraSvc._cameraParamSpecs[0];

            // First clear the recording objects in all the streams as we will set them up in the stream processing which follows.
            // Also set the absolute stream number
            camera.streams.forEach((stream: Stream) => {
                stream.recording.enabled = false
                stream.rec_num = recNo++;
            });
            // Process the streams
            camera.streams.forEach((stream, streamKey) => {
                if (stream.audio_encoding === "")
                    stream.audio_encoding = "None";

                if (isDevMode()) {  // Development mode
                    stream.media_server_input_uri = "http://localhost:8085/live/stream?suuid=cam" + camNum + "-stream" + streamNum;
                    stream.uri = "ws://localhost:8085/ws/stream?suuid=cam" + camNum + "-stream" + streamNum;
                    if (stream.netcam_uri === '')
                        stream.netcam_uri = 'rtsp://';

                    if (camera.recordingStream !== 'none' && camera.recordingStream === streamKey) {
                        stream.recording.enabled = true
                        stream.recording.recording_src_url = "http://localhost:8085/h/stream?suuid=cam" + camNum + "-stream" + streamNum;
                        stream.recording.uri = 'http://localhost:8084/recording/rec' + stream.rec_num + '/';
                        stream.recording.location = 'rec' + stream.rec_num;
                        stream.motion.trigger_recording_on = 'none';
                    } else if (stream.motion.enabled) {
                        // stream.recording = new Recording();
                        stream.recording.enabled = true;
                        stream.recording.recording_src_url = "http://localhost:8085/h/stream?suuid=cam" + camNum + "-stream" + streamNum;
                        stream.recording.uri = 'http://localhost:8084/recording/rec' + stream.rec_num + '/';
                        stream.recording.location = 'rec' + stream.rec_num;
                        if (stream.motion.trigger_recording_on !== 'none') {
                            let recStreamKey: string = stream.motion.trigger_recording_on;
                            // Get the key of the stream on which recordings are to be triggered
                            let recStream: Stream = camera.streams.get(recStreamKey) as Stream;
                            // Set up the recording
                            if (recStream !== undefined) {
                                recStream.recording.enabled = true;
                                recStream.recording.recording_src_url = "http://localhost:8085/h/stream?suuid=cam" + camNum + "-" + recStreamKey;
                                recStream.recording.uri = 'http://localhost:8084/recording/rec' + recStream.rec_num + '/';
                                recStream.recording.location = 'rec' + recStream.rec_num;
                            }
                        }
                    }
                } else {  // Production mode
                    stream.media_server_input_uri = "http://localhost:8085/live/stream?suuid=cam" + camNum + "-stream" + streamNum;
                    stream.uri = "/ws/stream?suuid=cam" + camNum + "-stream" + streamNum;
                    if (stream.netcam_uri === '')
                        stream.netcam_uri = 'rtsp://';
                    if (camera.recordingStream !== 'none' && camera.recordingStream === streamKey) {
                        stream.recording.enabled = true
                        stream.recording.recording_src_url = "http://localhost:8085/h/stream?suuid=cam" + camNum + "-stream" + streamNum;
                        stream.recording.uri = '/recording/rec' + stream.rec_num + '/';
                        stream.recording.location = 'rec' + stream.rec_num;
                        stream.motion.trigger_recording_on = 'none';
                    } else if (stream.motion.enabled) {
                        // stream.recording = new Recording();
                        stream.recording.enabled = true
                        stream.recording.recording_src_url = "http://localhost:8085/h/stream?suuid=cam" + camNum + "-stream" + streamNum;
                        stream.recording.uri = '/recording/rec' + stream.rec_num + '/';
                        stream.recording.location = 'rec' + stream.rec_num;
                        if (stream.motion.trigger_recording_on !== 'none') {
                            let recStreamKey: string = stream.motion.trigger_recording_on;
                            // Get the key of the stream on which recordings are to be triggered
                            let recStream: Stream = camera.streams.get(recStreamKey) as Stream;
                            // Set up the recording
                            if (recStream !== undefined) {
                                recStream.recording.enabled = true;
                                recStream.recording.recording_src_url = "http://localhost:8085/h/stream?suuid=cam" + camNum + "-" + recStreamKey;
                                recStream.recording.uri = '/recording/rec' + recStream.rec_num + '/';
                                recStream.recording.location = 'rec' + recStream.rec_num;
                            }
                        }
                    }
                }
                streamMap.set('stream' + streamNum, stream);
                ++streamNum;
            });
            streamNum = 1;
            camera.streams = streamMap;
            let newKey = 'camera' + camNum;
            retVal.set(newKey, camera);
            ++camNum;
        })

        this.cameras = retVal;
        this.setUpTableFormControls();
        //   this.cd.detectChanges();  // Fixes bug where the doorbell would come up with the wrong stream selected for motion after onvif discovery
    }

    toggle(el: { key: string, value: Camera }) {
        this.expandedElement = this.expandedElement === el.value ? null : el.value;
        this.showStreamDeleteConfirm = this.showCameraDeleteConfirm = this.camForRecordingSetup = '';
    }

    lastElement(cam: KeyValue<string, Camera>) {
        let key = Array.from(this.cameras.keys()).pop();
        return cam.key == key;
    }

    moveUp(cam: KeyValue<string, Camera>) {
        let prevKey: string = "";
        let gotPrevKey = false;
        this.cameras.forEach((v, k) => {
            if (k == cam.key)
                gotPrevKey = true;

            if (!gotPrevKey)
                prevKey = k;
        });
        let temp = this.cameras.get(prevKey);
        if (temp != undefined) {
            this.cameras.set(prevKey, cam.value);
            this.cameras.set(cam.key, temp);
        }
        this.FixUpCamerasData();
    }

    moveDown(cam: KeyValue<string, Camera>) {
        let nextKey: string = "";
        let getNextKey = false;
        this.cameras.forEach((v, k) => {
            if (getNextKey) {
                nextKey = k;
                getNextKey = false;
            }
            if (k == cam.key)
                getNextKey = true;
        });
        let temp = this.cameras.get(nextKey);
        if (temp != undefined) {
            this.cameras.set(nextKey, cam.value);
            this.cameras.set(cam.key, temp);
        }
        this.FixUpCamerasData();
    }

    setDefaultOnMultiDisplayStatus($event: MatCheckboxChange, stream: Stream, cam: Camera) {
        if ($event.checked) {   // Should only ever be checked as we disable the checkbox when it is checked to
            // always retain one stream set as the default
            // First clear the flag on all streams
            cam.streams.forEach((stream: Stream) => {
                stream.defaultOnMultiDisplay = false;
            });
            // Now set the selected one
            stream.defaultOnMultiDisplay = true;
        } else
            $event.source.checked = true;
    }

    setAudioInEnabledStatus($event: MatCheckboxChange, stream: Stream) {
        stream.audio = $event.checked;
        this.FixUpCamerasData();
    }

    async addCamera() {
        let newCamera: Camera = new Camera();
        let newStream: Stream = new Stream();
        newStream.defaultOnMultiDisplay = true; // Set the first stream defined for the camera to be
                                                // the multi cam display default
        newCamera.streams.set('stream1', newStream);
        this.cameras.set('camera' + (this.cameras.size + 1), newCamera);
        this.FixUpCamerasData();
    }

    addStream(cam: Camera) {
        cam.streams.set('stream' + (cam.streams.size + 1), new Stream())
        this.FixUpCamerasData();
    }

    anyInvalid(): boolean {
        let retVal: boolean = false;

        this.camControls.controls.forEach((camControlFormGroup: AbstractControl) => {
            if (camControlFormGroup.invalid)
                retVal = true;
        })

        if (!retVal)
            for (let streamFormArrayKey in this.streamControls) {
                this.streamControls[streamFormArrayKey].controls.forEach((streamControlFormGroup: AbstractControl) => {
                    if (streamControlFormGroup.invalid)
                        retVal = true;
                });
            }

        return retVal;
    }

    dataHasChanged(): boolean {
        return objectHash(this.cameras) !== this.savedDataHash;
    }

    restoreConfig() {
        this.ngOnInit();
    }

    commitConfig() {
        this.updating = true;
        this.reporting.dismiss();
        this.FixUpCamerasData();
        let cams: Map<string, Camera> = new Map(this.cameras);
        // First convert the map to JSON
        let jsonObj: {} = {};

        cams.forEach((cam, key: string) => {
            let newCam: Camera = JSON.parse(JSON.stringify(cam))
            let jsonStreams: {} = {};
            cam.streams.forEach((strValue, strKey: string) => {
                let newStream: Stream = JSON.parse(JSON.stringify(strValue))
                // @ts-ignore
                delete newStream.selected;
                // @ts-ignore
                delete newStream.rec_num;
                // @ts-ignore
                jsonStreams[strKey] = newStream;
            })
            // @ts-ignore
            newCam.streams = jsonStreams;
            // @ts-ignore
            jsonObj[key] = newCam;
        })

        this.cameraSvc.updateCameras(JSON.stringify(jsonObj)).subscribe(() => {
                this.reporting.successMessage = "Update Cameras Successful!";
                this.updating = false;
                // Update the saved data hash
                this.savedDataHash = objectHash(this.cameras);
                this.cd.detectChanges();
            },
            reason => {
                this.reporting.errorMessage = reason
                this.updating = false;
            }
        )
    }

    /**
     * createNew: Start new configuration with a single camera and stream
     */
    createNew() {
        this.cameras = new Map<string, Camera>();
        this.cameras.set('camera1', new Camera());
        let stream1: Stream = new Stream();
        stream1.defaultOnMultiDisplay = true;  // There must always be just one default on multi display so set it on the only stream.
        this.cameras.get('camera1')?.streams.set('stream1', stream1);
        this.FixUpCamerasData();
    }

    startOnvifSearch() {
        this.discovering = true;
        this.failed = new Map<string, string>();
        this.reporting.dismiss();
        this.cameraSvc.discover().subscribe((result: { cams: Map<string, Camera>, failed: Map<string, string> }) => {
                this.cameras = result.cams;
                this.discovering = false;
                this.FixUpCamerasData();
                this.failed = result.failed;
                if (this.cameras.size == 0)
                    this.reporting.warningMessage = "No cameras were found on this network"
            },
            reason => {
                this.reporting.errorMessage = reason;
                this.discovering = false;
            });
    }

    totalNumberOfStreams(): number {
        let totalStreams = 0;
        this.cameras.forEach((cam: Camera) => {
            cam.streams.forEach((stream: Stream) => {
                ++totalStreams;
            })
        })
        return totalStreams;
    }

    getSnapshot(cam: KeyValue<string, Camera>) {
        this.snapshotLoading = true;
        if (this.snapShotKey === cam.key)
            this.snapShotKey = '';
        else if (cam.value.snapshotUri !== '') {
            this.snapShotKey = cam.key;
            this.cameraSvc.getSnapshot(cam.value).subscribe(result => {
                    if (result !== null && result.body !== null) {
                        let ab = result.body.arrayBuffer()
                        ab.then((body => {
                            this.snapshot = this.sanitizer.bypassSecurityTrustResourceUrl('data:image/jpeg;base64,' + this.toBase64(body));
                            this.snapshotLoading = false;
                        }))
                    }
                },
                reason => {
                    if (reason.status === 401) {
                        this.reporting.warningMessage =
                            `Access to camera snapshot at ${cam.value.snapshotUri} is unauthorised. Please ensure the correct credentials for
              ${cam.key} is set. (Click on the shield icon on this camera row).`;
                    } else {
                        this.reporting.errorMessage = reason;
                    }
                    this.snapshotLoading = false;
                    this.snapShotKey = '';
                })
        }
    }

    toBase64(data: ArrayBuffer): string {
        let binary: string = '';
        let bytes: Uint8Array = new Uint8Array(data);
        let len: number = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    }

    checkIfOnvifCredentialsPresent() {
        this.cameraSvc.haveOnvifCredentials().subscribe(result => {
                this.haveOnvifCredentials = result == "true";
            },
            () => {
                this.reporting.errorMessage = new HttpErrorResponse({error: "Couldn't determine if camera credentials are set."});
            });
    }
    allOff() {
        this.camForRecordingSetup = this.camForCredentialsEntry = "";
        this.showCameraDeleteConfirm= this.showStreamDeleteConfirm = '';
        this.showAddCameraDialogue = this.showOnvifCredentialsForm = false;
    }

    togglePasswordDialogue(camId: string) {
        const camForCredentialsEntry = this.camForCredentialsEntry !== camId ? camId : ""
        this.allOff();
        this.camForCredentialsEntry = camForCredentialsEntry;
    }

    toggleOnvifPasswordDialogue() {
        const showOnvifCredentialsForm = !this.showOnvifCredentialsForm;
        this.allOff();
        this.showOnvifCredentialsForm = showOnvifCredentialsForm;
    }

    toggleAddCameraOnvifUriDialogue() {
        const showAddCameraDialogue = !this.showAddCameraDialogue;
        this.allOff();
        this.showAddCameraDialogue = showAddCameraDialogue;
    }

    toggleRecordingSetupDialogue(camId: string) {
        const camForRecordingSetup = this.camForRecordingSetup !== camId ? camId : "";
        this.allOff();
        this.camForRecordingSetup = camForRecordingSetup;

        if (this.camForRecordingSetup === camId)
            this.recordingSetupComponents.forEach((r) => {
                if (r.camKey === camId)
                    r.setupData();  // Reload recording setup component from main config data
            });
    }

    toggleCameraDeleteConfirm(key: string) {
        const showCameraDeleteConfirm = this.showCameraDeleteConfirm !== key ? key : '';
        this.allOff();
        this.showCameraDeleteConfirm = showCameraDeleteConfirm;
    }

    toggleStreamDeleteConfirm($event: { cam: string; stream: string }) {
        const compoundKey = $event.cam + $event.stream;
        const showStreamDeleteConfirm = this.showStreamDeleteConfirm !== compoundKey ? compoundKey : '';
        this.allOff();
        this.showStreamDeleteConfirm = showStreamDeleteConfirm;
    }

    startFindCameraDetails(onvifUrl: string) {
        this.gettingCameraDetails = true;
        this.cameraSvc.discoverCameraDetails(onvifUrl).subscribe((result: {
                cam: Camera,
                failed: Map<string, string>
            }) => {
                if (result.failed.size == 1) {
                    const fKey = result.failed.keys().next().value;
                    if (this.failed === undefined)
                        this.failed = result.failed;
                    else if (fKey !== undefined && !this.failed.has(fKey)) {
                        const fVal = result.failed.values().next().value;
                        if (fVal !== undefined)
                            this.failed.set(fKey, fVal);
                    }
                }
                if (result.cam !== undefined) {
                    this.cameras.set('camera' + (this.cameras.size + 1), result.cam);
                    this.FixUpCamerasData();
                }
                this.gettingCameraDetails = false;
            },
            reason => {
                this.reporting.errorMessage = reason;
                this.gettingCameraDetails = false;
            });
    }

    /**
     * toggleBackChannelAudio: Called when the back channel audio icon (camera row) is clicked on.
     *                         There is no validation as to whether 2 way audio is supported on the device.
     *                         Onvif device discovery sets this to the correct state initially, this gives a means
     *                         of overriding that result if required.
     * @param cam
     */
    toggleBackChannelAudio(cam: Camera) {
        cam.backchannelAudioSupported = !cam.backchannelAudioSupported;
    }

    deactivateConfirmed = false;
    newUri: string = '';

    canDeactivate(): CanDeactivateType {
        if (!this.dataHasChanged() && !this.anyInvalid())
            return true;
        else if (!this.deactivateConfirmed) {
            this.checkDeactivate = true;
            this.newUri = window.location.href;
        } else if (this.deactivateConfirmed) {
            this.deactivateConfirmed = this.checkDeactivate = false;
            return true;
        }
        return false;
    }

    confirmDeactivate($event: boolean) {
        this.checkDeactivate = false;
        this.deactivateConfirmed = $event;
        if (this.deactivateConfirmed)
            window.location.href = this.newUri;
    }

    ngOnInit(): void {
        // Set up the available streams/cameras for selection by the checkboxes
        this.cameraSvc.loadCameras().subscribe(cameras => {
                this.cameras = cameras;

                this.downloading = false;
                this.FixUpCamerasData()
                this.savedDataHash = objectHash(this.cameras);
            },
            () => {
                this.createNew();
                this.reporting.errorMessage = new HttpErrorResponse({error: 'The configuration file is absent, empty or corrupt. Please set up the configuration for your cameras and save it.'});
                this.downloading = false;
            })
        this.checkIfOnvifCredentialsPresent();
        this.isGuest = this.utils.isGuestAccount;
    }

    ngAfterViewInit(): void {
    }

    ngOnDestroy() {
    }

    streamDeleteButtonToolTip(cam:{value: Camera, key: string}, stream: string) {
        return cam.value.streams.size <= 1 ? 'Cannot delete last stream' : this.getStreamDeleteDisabledState(cam, stream) ? 'Cannot delete a stream used in recording' : 'Delete this stream';
    }
}
