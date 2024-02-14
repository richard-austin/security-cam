import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  isDevMode,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, CameraParamSpec, Stream} from "../cameras/Camera";
import {ReportingComponent} from '../reporting/reporting.component';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup, NgControl,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";
import {BehaviorSubject} from 'rxjs';
import {MatCheckboxChange} from "@angular/material/checkbox";
import {MatSelectChange} from '@angular/material/select/select';
import {HttpErrorResponse} from "@angular/common/http";
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {KeyValue} from '@angular/common';
import {UtilsService} from '../shared/utils.service';

declare let objectHash: (obj: Object) => string;

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
})
export class ConfigSetupComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('errorReporting') reporting!: ReportingComponent;
  @ViewChild('outputframeid') snapshotImage!: ElementRef<HTMLImageElement>
  @ViewChild('scrollable_content') scrollableContent!: ElementRef<HTMLElement>
  downloading: boolean = true;
  updating: boolean = false;
  discovering: boolean = false;
  cameras: Map<string, Camera> = new Map<string, Camera>();
  cameraColumns = ['sorting', 'camera_id', 'delete', 'expand', 'name', 'cameraParamSpecs', 'ftp', 'retrigger-window', 'address', 'snapshotUri', 'useRtspAuth', 'rtspTransport', 'backchannelAudioSupported', 'ptzControls', 'onvifHost'];
  cameraFooterColumns = ['buttons'];

  expandedElement!: Camera | null;
  streamColumns = ['stream_id', 'delete', 'descr', 'audio', 'audio_encoding', 'netcam_uri', 'defaultOnMultiDisplay', 'motion', 'threshold', 'trigger_recording_on', 'preambleFrames', 'mask_file', 'video_width', 'video_height'];
  streamFooterColumns = ['buttons']
//  camSetupFormGroup!: FormGroup;
  camControls!: FormArray;
  streamControls: FormArray[] = [];
  list$!: BehaviorSubject<Camera[]>;
  confirmSave: boolean = false;
  confirmNew: boolean = false;
  confirmNewLookup: boolean = false;
  snapshotLoading: boolean = false;
  snapshot: SafeResourceUrl | String = '';
  snapShotKey: string = '';
  showPasswordDialogue: boolean = false;
  showAddCameraDialogue: boolean = false;
  isGuest: boolean = true;
  gettingCameraDetails: boolean = false;
  savedDataHash: string = "";
  haveCameraCredentials: boolean = false;

  constructor(public cameraSvc: CameraService, private utils: UtilsService, private sanitizer: DomSanitizer, private cd: ChangeDetectorRef) {
  }

  getCamControl(index: number, fieldName: string): FormControl {
    return this.camControls.at(index).get(fieldName) as FormControl;
  }

  setPTZControlsCheckboxDisabledState(index: number): boolean {
    let ptzc: FormControl = this.getCamControl(index, 'ptzControls');

    let ovhc: FormControl = this.getCamControl(index, 'onvifHost')
    if (ovhc.value == '')
      ptzc.setValue(false);  // Ensure PTZ is set to "off" if onvifHost has the (valid) value empty
    return ovhc.value == '' || !ovhc.valid;
  }
  getPreambleFramesDisabledState(cam: Camera, stream: Stream): boolean {
    return !stream?.motion?.enabled && !cam?.ftp;
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

  getStreamControl(camIndex: number, streamIndex: number, fieldName: string): FormControl {
    return this.streamControls[camIndex].at(streamIndex).get(fieldName) as FormControl;
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

  getFTPDisabledState(camera: Camera): boolean {
    if (camera?.cameraParamSpecs?.camType === undefined)
      return true;
    else
      return this.motionSet(camera);
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
        return new FormGroup({
          descr: new FormControl({
            value: stream.descr,
            disabled: false
          }, [Validators.required, Validators.maxLength(20), Validators.pattern(/^[a-zA-Z0-9\\ ]{2,20}$/)]),
          audio: new FormControl(stream.audio, [Validators.required]),
          audio_encoding: new FormControl(stream.audio_encoding, [Validators.required, Validators.pattern(/^(AAC|G711|G726|None|Not Listed)$/)]),
          netcam_uri: new FormControl(stream.netcam_uri, [Validators.pattern(/\b((rtsp):\/\/[-\w]+(\.\w[-\w]*)+|(?:[a-z0-9](?:[-a-z0-9]*[a-z0-9])?\.)+(?: com\b|edu\b|biz\b|gov\b|in(?:t|fo)\b|mil\b|net\b|org\b|[a-z][a-z]\b))(\\:\d+)?(\/[^.!,?;"'<>()\[\]{}\s\x7F-\xFF]*(?:[.!,?]+[^.!,?;"'<>()\[\]{}\s\x7F-\xFF]+)*)?/)]),
          video_width: new FormControl({
            value: stream.video_width,
            disabled: !stream.motion?.enabled
          }, [Validators.required, Validators.min(90), Validators.max(5000)]),
          video_height: new FormControl({
            value: stream.video_height,
            disabled: !stream.motion?.enabled
          }, [Validators.required, Validators.min(90), Validators.max(3000)]),
          //  enabled: new FormControl(stream.motion.enabled, [Validators.nullValidator]),
          threshold: new FormControl({
            value: stream.motion?.threshold != undefined ? stream.motion.threshold : 1500,
            disabled: !stream.motion.enabled
          }, [Validators.required, Validators.min(1), Validators.max(2147483647)]),
          trigger_recording_on: new FormControl({
            value: stream.motion.trigger_recording_on,
            disabled: !stream.motion.enabled
          }, [Validators.nullValidator]),
          preambleFrames: new FormControl({
            value: stream.preambleFrames,
            disabled: this.getPreambleFramesDisabledState(camera, stream),
          }, [Validators.min(0), Validators.max(300)]),
          mask_file: new FormControl({
            value: stream.motion.mask_file,
            disabled: !stream.motion.enabled
          }, [isValidMaskFileName(this.cameras), Validators.maxLength(55)])
        }, {updateOn: "change"});
      });

      this.streamControls.push(new FormArray(toStreamGroups));
      return new FormGroup({
        name: new FormControl(camera.name, [Validators.required, Validators.maxLength(25)]),
        address: new FormControl({
          value: camera.address,
          disabled: this.getCameraAddressDisabledState(camera)
        }, [Validators.pattern(/\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}\b/)]),
        cameraParamSpecs: new FormControl({
          value: this.getCameraParamSpecsReferenceCopy(camera),
          disabled: false
        }, [Validators.maxLength(55)]),
        ftp: new FormControl({
          value: camera.ftp,
          disabled: false,
        }, [validateTrueOrFalse({ftp: true})]),
        retriggerWindow: new FormControl({
            value: camera?.retriggerWindow != undefined ? camera.retriggerWindow : 30,
            disabled: false,
          }, [Validators.pattern(/^10$|20|30|40|50|60|70|80|90|100/)]
        ),
        snapshotUri: new FormControl({
          value: camera.snapshotUri,
          disabled: false
        }, [Validators.maxLength(150)]),
        ptzControls: new FormControl({
          value: camera.ptzControls,
          disabled: false
        }, [validateTrueOrFalse({ptzControls: true})]),
        onvifHost: new FormControl({
          value: camera.onvifHost,
          disabled: false,
        }, [Validators.maxLength(22),
          Validators.pattern(/^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))($|:([0-9]{1,4}|6[0-5][0-5][0-3][0-5])$)/)]),
        useRtspAuth: new FormControl({
          value: camera.useRtspAuth == undefined ? false : camera.useRtspAuth,
          disabled: false,
        }, [validateTrueOrFalse({useRtspAuth: true})]),
        rtspTransport: new FormControl({
          value: camera.rtspTransport,
          disabled: false,
        }, [Validators.required, Validators.pattern(/^(udp|tcp)$/)])
      }, {updateOn: "change"});
    });
    this.camControls = new FormArray(toCameraGroups);

    // Ensure camera form controls highlight immediately if invalid
    for (let i = 0; i < this.camControls.length; ++i) {
      this.camControls.at(i).markAllAsTouched();
    }

    // Ensure stream form controls highlight immediately if invalid
    this.streamControls.forEach((fa: FormArray) => {
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
   * @param cameraKey
   * @param streamKey
   */
  deleteStream(cameraKey: string, streamKey: string): boolean {
    let retVal: boolean = false;

    let cam: Camera = this.cameras.get(cameraKey) as Camera;
    if (cam !== undefined) {
      retVal = Array.from(cam.streams.keys()).find(k => k === streamKey) !== undefined;
      if (retVal) {
        let enableFirstAsMultiDisplayDefault: boolean = (cam.streams.get(streamKey) as Stream).defaultOnMultiDisplay;
        cam.streams.delete(streamKey);
        cam.streams.forEach((stream) => {
          // Ensure we don't land up with none selected as default stream to show on multi cameras display
          if (enableFirstAsMultiDisplayDefault) {
            stream.defaultOnMultiDisplay = enableFirstAsMultiDisplayDefault;
            enableFirstAsMultiDisplayDefault = false;
          }
          if (stream.motion.enabled)
            stream.motion.trigger_recording_on = '';  // Set all recording triggers to 'None' as the stream keys may be renumbered
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
    let streamNum: number = 1;  // Absolute (not local to camera) number to identify stream number
                                // for recording and live URL's
    let retVal: Map<string, Camera> = new Map<string, Camera>();
    let recNo: number = 1;  // Recording number to be set in the stream object
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
      let streamKeyNum: number = 1;
      // Process the streams
      camera.streams.forEach((stream) => {
        if (stream.audio_encoding === "")
          stream.audio_encoding = "None";

        if (isDevMode()) {  // Development mode
          stream.media_server_input_uri = "http://localhost:8085/live/stream?suuid=stream" + streamNum;
          stream.uri = "ws://localhost:8085/ws/stream?suuid=stream" + streamNum;
          if (stream.netcam_uri === '')
            stream.netcam_uri = 'rtsp://';

          if (camera.ftp && streamKeyNum++ == 1) {
            stream.recording.enabled = true
            stream.recording.recording_src_url = 'http://localhost:8085/h/stream?suuid=stream' + streamNum;
            stream.recording.uri = 'http://localhost:8084/recording/rec' + streamNum + '/';
            stream.recording.location = 'rec' + streamNum;
            stream.motion.trigger_recording_on = '';
          } else if (stream.motion.enabled) {
            // stream.recording = new Recording();
            stream.recording.enabled = true;
            stream.recording.recording_src_url = 'http://localhost:8085/h/stream?suuid=stream' + streamNum;
            stream.recording.uri = 'http://localhost:8084/recording/rec' + streamNum + '/';
            stream.recording.location = 'rec' + streamNum;
            if (stream.motion.trigger_recording_on !== '') {
              let recStreamKey: string[] = stream.motion.trigger_recording_on.split('.');
              if (recStreamKey.length === 2) {
                // Get the key of the stream on which recordings are to be triggered
                let recStream: Stream = camera.streams.get(recStreamKey[1]) as Stream;
                // Set up the recording
                if (recStream !== undefined) {
                  recStream.recording.enabled = true;
                  recStream.recording.recording_src_url = 'http://localhost:8085/h/stream?suuid=stream' + recStream.rec_num;
                  recStream.recording.uri = 'http://localhost:8084/recording/rec' + recStream.rec_num + '/';
                  recStream.recording.location = 'rec' + recStream.rec_num;
                }
              }
            }
          }
        } else {  // Production mode
          stream.media_server_input_uri = "http://localhost:8085/live/stream?suuid=stream" + streamNum;
          stream.uri = "/ws/stream?suuid=stream" + streamNum;
          if (stream.netcam_uri === '')
            stream.netcam_uri = 'rtsp://';
          if (camera.ftp && streamKeyNum++ === 1) {
            stream.recording.enabled = true
            stream.recording.recording_src_url = 'http://localhost:8085/h/stream?suuid=stream' + streamNum;
            stream.recording.uri = '/recording/rec' + streamNum + '/';
            stream.recording.location = 'rec' + streamNum;
            stream.motion.trigger_recording_on = '';
          } else if (stream.motion.enabled) {
            // stream.recording = new Recording();
            stream.recording.enabled = true
            stream.recording.recording_src_url = 'http://localhost:8085/h/stream?suuid=stream' + streamNum;
            stream.recording.uri = '/recording/rec' + streamNum + '/';
            stream.recording.location = 'rec' + streamNum;
            if (stream.motion.trigger_recording_on !== '') {
              let recStreamKey: string[] = stream.motion.trigger_recording_on.split('.');
              if (recStreamKey.length === 2)  // Should have a camera and stream number
              {
                // Get the key of the stream on which recordings are to be triggered
                let recStream: Stream = camera.streams.get(recStreamKey[1]) as Stream;
                // Set up the recording
                if (recStream !== undefined) {
                  recStream.recording.enabled = true;
                  recStream.recording.recording_src_url = 'http://localhost:8085/h/stream?suuid=stream' + recStream.rec_num;
                  recStream.recording.uri = '/recording/rec' + recStream.rec_num + '/';
                  recStream.recording.location = 'rec' + recStream.rec_num;
                }
              }
            }
          }
        }
        streamMap.set('stream' + streamNum, stream);
        ++streamNum;
      })
      camera.streams = streamMap;
      let newKey = 'camera' + camNum;
      retVal.set(newKey, camera);
      ++camNum;
    })

// Renumber trigger_recording_on references so that the camera number is always the same as the camera key
// Deleting a camera other than the last will cause the camera keys not to tie up with any previously set
// reference in trigger_recording_on.
    retVal.forEach((camera: Camera, camKey: string) => {
      camera.streams.forEach((stream: Stream) => {
        if (stream.motion.trigger_recording_on !== '') {
          let fields: string[] = stream.motion.trigger_recording_on.split('.');
          fields[0] = camKey;
          stream.motion.trigger_recording_on = fields[0] + '.' + fields[1];
        }
      })
    })

    this.cameras = retVal;
    this.setUpTableFormControls();
    this.cd.detectChanges();  // Fixes bug where the doorbell would come up with the wrong stream selected for motion after onvif discovery
  }

  toggle(el: { key: string, value: Camera }) {
    this.expandedElement = this.expandedElement === el.value ? null : el.value;
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

  addCamera() {
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

    if (retVal)
      return retVal;

    for (let streamFormArrayKey in this.streamControls) {
      this.streamControls[streamFormArrayKey].controls.forEach((streamControlFormGroup: AbstractControl) => {
        if (streamControlFormGroup.invalid)
          retVal = true;
      })
    }

    return retVal;
  }

  dataHasChanged(): boolean {
    return objectHash(this.cameras) !== this.savedDataHash;
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
    this.cameraSvc.discover().subscribe((cams: Map<string, Camera>) => {
        this.cameras = cams;
        this.discovering = false;
        this.FixUpCamerasData();
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
        if (stream.recording.enabled)
          ++totalStreams;
      })
    })
    return totalStreams;
  }

  uploadMaskFile($event: Event, camKey: string, camIndex: number, streamIndex: number) {
    let fileUploadInput: HTMLInputElement = $event.target as HTMLInputElement;
    if (fileUploadInput.files && fileUploadInput.files.length > 0) {
      let stream: Stream = Array.from(  // Streams
        Array.from( // Cameras
          this.cameras.values())[camIndex].streams.values())[streamIndex];

      stream.motion.mask_file = fileUploadInput?.files[0].name;

      let control: FormControl = this.getStreamControl(camIndex, streamIndex, 'mask_file');
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

  getSnapshot(cam: KeyValue<string, Camera>) {
    this.snapshotLoading = true;
    if (this.snapShotKey === cam.key)
      this.snapShotKey = '';
    else if (cam.value.snapshotUri !== '') {
      this.snapShotKey = cam.key;
      this.cameraSvc.getSnapshot(cam.value.snapshotUri).subscribe(result => {
          this.snapshot = this.sanitizer.bypassSecurityTrustResourceUrl('data:image/jpeg;base64,' + this.toBase64(result));
          this.snapshotLoading = false;
        },
        reason => {
          if (reason.status === 401) {
            this.reporting.warningMessage =
              `Access to camera snapshot at ${cam.value.snapshotUri} is unauthorised. Please ensure the correct credentials for
              all cameras are set. (Click on the shield icon to the right of this page title).`;
          } else {
            this.reporting.errorMessage = reason;
          }
          this.snapshotLoading = false;
          this.snapShotKey = '';
        })
    }
  }

  ftpSet(cam: Camera): boolean {
    return cam.ftp;
  }

  motionSet(cam: Camera): boolean {
    let hasMotionSet: boolean = false;
    if (cam?.streams !== undefined) {
      for (let stream of cam.streams.values()) {
        if (stream?.motion?.enabled !== undefined && stream.motion.enabled) {
          hasMotionSet = true;
          break;
        }
      }
    }
    return hasMotionSet;
  }

  toBase64(data: Array<any>): string {
    let binary: string = '';
    let bytes: Uint8Array = new Uint8Array(data);
    let len: number = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }

  private checkIfCameraCredentialsPresent() {
    this.cameraSvc.haveCameraCredentials().subscribe(result => {
        this.haveCameraCredentials = result == "true";
      },
      () => {
        this.reporting.errorMessage = new HttpErrorResponse({error: "Couldn't determine if camera credentials are set."});
      });
  }

  togglePasswordDialogue() {
    this.showPasswordDialogue = !this.showPasswordDialogue;
    this.showAddCameraDialogue = false;
    this.checkIfCameraCredentialsPresent();
  }

  toggleAddCameraOnvifUriDialogue() {
    this.showAddCameraDialogue = !this.showAddCameraDialogue;
    this.showPasswordDialogue = false;
  }

  startFindCameraDetails(onvifUrl: string) {
    this.gettingCameraDetails = true;
    this.cameraSvc.discoverCameraDetails(onvifUrl).subscribe((cam: Camera) => {
        this.cameras.set('camera' + (this.cameras.size + 1), cam);
        this.FixUpCamerasData();
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

  getScrollableContentStyle():string {
    const scrollableContent = this.scrollableContent?.nativeElement;

    if(scrollableContent !== undefined ) {
      const boundingRect = scrollableContent.getBoundingClientRect()
      return `width: 100%; height: calc(100dvh - ${boundingRect.top+17}px); overflow: auto;`
    }
    else return ""
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
    this.checkIfCameraCredentialsPresent();
    this.isGuest = this.utils.isGuestAccount;
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy() {
  }
}
