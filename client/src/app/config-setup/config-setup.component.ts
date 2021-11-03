import {AfterViewInit, Component, isDevMode, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, Stream} from "../cameras/Camera";
import {ReportingComponent} from '../reporting/reporting.component';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";
import {BehaviorSubject} from 'rxjs';
import {MatCheckboxChange} from "@angular/material/checkbox";
import {MatSelectChange} from '@angular/material/select/select';
import {HttpErrorResponse} from "@angular/common/http";

export function isValidIP(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {

    const value = control.value;

    if (!value) {
      return {address: true};
    }
    // Camera IP address is required if control URI is defined.
    const addressValid = /\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}\b/.test(value);

    return !addressValid ? {address: true} : null;
  }
}

export function isValidNetCamURI(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {

    const value = control.value;

    if (!value) {
      return null;
    }

    const uriValid = RegExp('\\b((rtsp):\\/\\/[-\\w]+(\\.\\w[-\\w]*)+|(?:[a-z0-9](?:[-a-z0-9]*[a-z0-9])?\\.)+(?: com\\b|edu\\b|biz\\b|gov\\b|in(?:t|fo)\\b|mil\\b|net\\b|org\\b|[a-z][a-z]\\b))(\\\\:\\d+)?(\\/[^.!,?;"\'<>()\\[\\]{}\\s\x7F-\xFF]*(?:[.!,?]+[^.!,?;"\'<>()\\[\\]{}\\s\x7F-\xFF]+)*)?').test(value);

    return !uriValid ? {netcam_uri: true} : null;
  }
}

export function isValidMaskFileName(cameras:Map<string, Camera>): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {

    const value = control.value;

    if (!value) {
      return null;
    }

    let allFiles:Set<string> = new Set<string>();
    let duplicateMaskFile: boolean = false;

    const fileNameValid = RegExp('^[a-zA-Z0-9-_]+.pgm$').test(value);
    // Check that no file name is being used by more than one camera
    cameras.forEach((cam:Camera) => {
      cam.streams.forEach((stream:Stream) => {
        if(stream.motion.enabled) {
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
export class ConfigSetupComponent implements OnInit, AfterViewInit {
  @ViewChild('errorReporting') reporting!: ReportingComponent;

  downloading: boolean = true;
  updating: boolean = false;
  cameras: Map<string, Camera> = new Map<string, Camera>();
  cameraColumns = ['camera_id', 'delete', 'expand', 'name', 'controlUri', 'address'];
  cameraFooterColumns = ['buttons'];

  expandedElement!: Camera | null;
  streamColumns = ['stream_id', 'delete', 'descr', 'netcam_uri', 'defaultOnMultiDisplay', 'motion', 'trigger_recording_on', 'mask_file', 'video_width', 'video_height'];
  streamFooterColumns = ['buttons']
//  camSetupFormGroup!: FormGroup;
  camControls!: FormArray;
  streamControls: FormArray[] = [];
  list$!: BehaviorSubject<Camera[]>;
  confirmSave: boolean = false;
  confirmNew: boolean = false;

  constructor(private cameraSvc: CameraService) {
  }

  getCamControl(index: number, fieldName: string): FormControl {
    return this.camControls.at(index).get(fieldName) as FormControl;
  }

  updateCam(index: number, field: string, value: any) {
    console.log(index, field, value);

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

  updateMotion(camIndex: number, streamIndex: number, field: string, value: any) {
    Array.from(  // Streams
      Array.from( // Cameras
        this.cameras.values())[camIndex].streams.values()).forEach((stream: Stream, i) => {
      if (i === streamIndex) { // @ts-ignore
        stream['motion'][field] = value;
      }
    });
  }

  updateMotionField(camIndex: number, streamIndex: number, field: string) {
    const control = this.getStreamControl(camIndex, streamIndex, field);
    if (control) {
      this.updateMotion(camIndex, streamIndex, field, control.value);
    }
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
          }, [Validators.required, Validators.maxLength(25)]),
          netcam_uri: new FormControl(stream.netcam_uri, [Validators.required, isValidNetCamURI()]),
          video_width: new FormControl({
            value: stream.video_width,
            disabled: !stream.motion?.enabled
          }, [Validators.required, Validators.min(90), Validators.max(5000)]),
          video_height: new FormControl({
            value: stream.video_height,
            disabled: !stream.motion?.enabled
          }, [Validators.required, Validators.min(90), Validators.max(3000)]),
          //  enabled: new FormControl(stream.motion.enabled, [Validators.nullValidator]),
          trigger_recording_on: new FormControl({
            value: stream.motion.trigger_recording_on,
            disabled: !stream.motion.enabled
          }, [Validators.nullValidator]),
          mask_file: new FormControl({
            value: stream.motion.mask_file,
            disabled: !stream.motion.enabled
          }, [isValidMaskFileName(this.cameras), Validators.maxLength(55)])
        }, {updateOn: "change"});
      });

      this.streamControls.push(new FormArray(toStreamGroups));
      return new FormGroup({
        name: new FormControl(camera.name, [Validators.required, Validators.maxLength(25)]),
        address: new FormControl({value: camera.address, disabled: camera.controlUri.length == 0}, [isValidIP()]),
        controlUri: new FormControl({
          value: camera.controlUri,
          disabled: false
        }, [Validators.maxLength(55)]),
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
   * deleteCamera: Delete a camera from the cameras.map
   * @param key: The key of the map entry to be deleted
   */
  deleteCamera(key: string): boolean {
    let retVal: boolean = Array.from(this.cameras.keys()).find(k => k === key) !== undefined;
    this.cameras.delete(key);
    this.FixUpCamerasData();
    return retVal;
  }

  /**
   * deleteStream: Delete a stream from the streams.map
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
            stream.motion.trigger_recording_on = '';  // Set all recording triggers to 'None' as the the stream keys may be renumbered
        })
      }
      this.FixUpCamerasData();
    }
    return retVal;
  }

  /**
   * FixUpCamerasData: Fix the key names in the cameras and streams maps so they follow the sequence
   *                   camera1, camera2 or stream1, stream 2 etc. This is run after deleting an item
   *                   from the map. Also number the live streams and recording uri's logically
   */
  FixUpCamerasData(): void {
    let camNum: number = 1;
    let streamNum: number = 1;  // Absolute (not local to camera) number to identify stream number
                                // for recording and live URL's
    let retVal: Map<string, Camera> = new Map<string, Camera>();
    let absoluteStreamNo: number = 1;  // Absolute number to be set in the stream object
    this.cameras.forEach((camera: Camera) => {
      let streamMap: Map<string, Stream> = new Map<string, Stream>();
      let streamKeyNum: number = 1;

      // First clear the recording objects in all the streams as we will set them up in the stream processing which follows.
      // Also set the absolute stream number
      camera.streams.forEach((stream: Stream) => {
        stream.recording.enabled = false
        stream.absolute_num = absoluteStreamNo++;

      });
      // Process the streams
      camera.streams.forEach((stream) => {
        if (isDevMode()) {  // Development mode
          stream.nms_uri = "rtmp://localhost:1935/nms/stream" + streamNum;
          stream.uri = "http://localhost:8009/nms/stream" + streamNum + ".flv";
          if (stream.netcam_uri === '')
            stream.netcam_uri = 'rtsp://';

          if (stream.motion.enabled) {
            // stream.recording = new Recording();
            stream.recording.enabled = true;
            stream.recording.uri = 'http://localhost:8084/recording/stream' + streamNum + '/';
            stream.recording.location = 'stream' + streamNum;
            if (stream.motion.trigger_recording_on !== '') {
              let recStreamKey: string[] = stream.motion.trigger_recording_on.split('.');
              if (recStreamKey.length === 2) {
                // Get the key of the stream on which recordings are to be triggered
                let recStream: Stream = camera.streams.get(recStreamKey[1]) as Stream;
                // Set up the recording
                if (recStream !== undefined) {
                  recStream.recording.enabled = true;
                  recStream.recording.uri = 'http://localhost:8084/recording/stream' + recStream.absolute_num + '/';
                  recStream.recording.location = 'stream' + recStream.absolute_num;
                }
              }
            }
          }
        } else {  // Production mode
          stream.nms_uri = "rtmp://localhost:1935/nms/stream" + streamNum;
          stream.uri = "/live/nms/stream" + streamNum + ".flv";
          if (stream.netcam_uri === '')
            stream.netcam_uri = 'rtsp://';
          if (stream.motion.enabled) {
            // stream.recording = new Recording();
            stream.recording.enabled = true
            stream.recording.uri = '/recording/stream' + streamNum + '/';
            stream.recording.location = 'stream' + streamNum;
            if (stream.motion.trigger_recording_on !== '') {
              let recStreamKey: string[] = stream.motion.trigger_recording_on.split('.');
              if (recStreamKey.length === 2)  // Should have a camera and stream number
              {
                // Get the key of the stream on which recordings are to be triggered
                let recStream: Stream = camera.streams.get(recStreamKey[1]) as Stream;
                // Set up the recording
                if (recStream !== undefined) {
                  recStream.recording.enabled = true;
                  recStream.recording.uri = '/recording/stream' + recStream.absolute_num + '/';
                  recStream.recording.location = 'stream' + recStream.absolute_num;
                }
              }
            }
          }
        }
        streamMap.set('stream' + streamKeyNum, stream);
        ++streamNum;
        ++streamKeyNum;
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
  }

  toggle(el: { key: string, value: Camera }) {
    this.expandedElement = this.expandedElement === el.value ? null : el.value;
  }

  /**
   * setMotionStatus: Enable/disable motion sensing on the stream according to the checkbox state.
   * @param $event: MatCheckboxChange event
   * @param stream: The stream
   * @param cam: The  parent camera
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
        delete newStream.absolute_num;
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
        this.cameraSvc.configUpdated();  // Tell nav component to reload the camera data
        this.updating = false;
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

  uploadMaskFile($event: Event, camKey:string, camIndex: number, streamIndex: number) {
    let fileUploadInput: HTMLInputElement = $event.target as HTMLInputElement;
    if (fileUploadInput.files && fileUploadInput.files.length > 0) {
      let stream: Stream = Array.from(  // Streams
        Array.from( // Cameras
          this.cameras.values())[camIndex].streams.values())[streamIndex];

      stream.motion.mask_file = fileUploadInput.files[0].name;

      let control: FormControl = this.getStreamControl(camIndex, streamIndex, 'mask_file');
      control.setValue(stream.motion.mask_file);
      if(control.valid) {
        // Upload file to server
        this.cameraSvc.uploadMaskFile(fileUploadInput.files[0])
          .subscribe(() => {
              this.reporting.successMessage = stream.motion.mask_file + ' uploaded successfully'
            },
            (reason) => {
              this.reporting.errorMessage = reason
            });
      }
      else
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

  ngOnInit(): void {
    // Set up the available streams/cameras for selection by the check boxes
    this.cameraSvc.loadCameras().subscribe(cameras => {
        this.cameras = cameras;
        this.FixUpCamerasData()
        this.downloading = false;
      },
      () => {
        this.createNew();
        this.reporting.errorMessage = new HttpErrorResponse({error: 'The configuration file is absent, empty or corrupt. Please set up the configuration for your cameras and save it.'});
        this.downloading = false;
      })
  }

  ngAfterViewInit(): void {
  }
}
