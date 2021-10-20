import {AfterViewInit, Component, isDevMode, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera, Stream} from "../cameras/Camera";
import {ReportingComponent} from '../reporting/reporting.component';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {BehaviorSubject} from 'rxjs';

export interface Data {
  name: string;
  address: string;
  controlUri: string;
  another: string;
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
  @ViewChild('errorReporting') errorReporting!: ReportingComponent;
  downloading: boolean = true;
  cameras: Map<string, Camera> = new Map<string, Camera>();
  displayedColumns = ['delete', 'expand', 'name', 'address1', 'controlUri'];
  expandedElement!: Camera | null;
  streamColumns = ['delete', 'descr', 'netcam_uri', 'uri', 'nms_uri', 'video_width', 'video_height'];
//  camSetupFormGroup!: FormGroup;
  controls!: FormArray;
  streamControls: FormArray[] = [];

  list$!: BehaviorSubject<Camera[]>;

  constructor(private cameraSvc: CameraService) {
  }

  getCamControl(index: number, fieldName: string): FormControl {
    return this.controls.at(index).get(fieldName) as FormControl;
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
    if (control.valid) {
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
    if (control.valid) {
      this.updateStream(camIndex, streamIndex, field, control.value);
    }
  }

  /**
   * setUpTableFormControls: Associate a FormControl with each editable field on the table
   */
  setUpTableFormControls(): void {
    this.list$ = new BehaviorSubject<Camera[]>(Array.from(this.cameras.values()));
    let index: number = 0;
    const toCameraGroups = this.list$.value.map(camera => {
      let list$: BehaviorSubject<Stream[]> = new BehaviorSubject<Stream[]>(Array.from(camera.streams.values()));
      const toStreamGroups = list$.value.map((stream: Stream) => {
        return new FormGroup({
          descr: new FormControl(stream.descr, [Validators.required, Validators.maxLength(25)]),
          netcam_uri: new FormControl(stream.netcam_uri, [Validators.required, Validators.maxLength(40)]),
        }, {updateOn: "blur"});
      });
      this.streamControls[index++] = new FormArray(toStreamGroups);

      return new FormGroup({
        name: new FormControl(camera.name, [Validators.required, Validators.maxLength(25)]),
      }, {updateOn: "blur"});
    });

    this.controls = new FormArray(toCameraGroups);
  }

  /**
   * deleteCamera: Delete a camera from the cameras.map
   * @param key: The key of the map entry to be deleted
   */
  deleteCamera(key: string): boolean
  {
    let retVal: boolean = Array.from(this.cameras.keys()).find(k => k === key) !== undefined;
    this.cameras.delete(key);
    this.cameras = this.fixKeyNames(this.cameras) as Map<string, Camera>;
    this.setUpTableFormControls();
    return retVal;
  }

  /**
   * deleteStream: Delete a stream from the streams.map
   * @param cameraKey
   * @param streamKey
   */
  deleteStream(cameraKey: string, streamKey: string): boolean
  {
    let retVal: boolean = false;

    let cam:Camera = this.cameras.get(cameraKey) as Camera;
    if(cam !== undefined)
    {
      retVal = Array.from(cam.streams.keys()).find(k => k === streamKey) !== undefined;
      if(retVal)
      cam.streams.delete(streamKey);
      cam.streams = this.fixKeyNames(cam.streams) as Map<string, Stream>;
    }
    this.setUpTableFormControls();
    return retVal;
  }

  /**
   * fixKeyNames: Fix the key names in the cameras or streams maps so they follow the sequence
   *              camera1, camera2 or stream1, stream 2 etc. This is run after deleting an iyem
   *              from the map.
   */
  fixKeyNames(map:Map<string, Camera | Stream>):Map<string, Camera | Stream>
  {
    let index: number = 1;
    let baseName: string;
    let streamNum: number | undefined = undefined;

    if((map.size) > 0 && (map.values().next().value).streams !== undefined)
      baseName = 'camera';
    else {
      baseName = 'stream';
      streamNum = 1;
    }
    let retVal: Map<string, Camera | Stream> = new Map<string, Camera | Stream>();

    map.forEach((value:Camera| Stream) => {
      let newKey = baseName+index++;
      retVal.set(newKey, value);
      if(streamNum !== undefined)
      {
        if ((value instanceof Stream) && isDevMode()) {
          value.nms_uri = "http://localhost:8009/nms/stream"+streamNum;
          value.uri = "http://localhost:8009/nms/stream"+streamNum++ +".flv";
        }
        else if((value instanceof Stream))
        {
          value.nms_uri = "http://localhost:8009/nms/stream"+streamNum;
          value.uri = "/live/nms/stream"+streamNum++ +".flv";

        }
      }
    })
    return retVal;
  }

  ngOnInit(): void {
    // Set up the available streams/cameras for selection by the check boxes
    this.cameraSvc.loadCameras().subscribe(cameras => {
        this.cameras = cameras;
        this.downloading = false;
        this.setUpTableFormControls();
      },
      reason => this.errorReporting.errorMessage = reason);
  }

  ngAfterViewInit(): void {
  }

  toggle(el: { key: string, value: Camera }) {
    this.expandedElement = this.expandedElement === el.value ? null : el.value;
  }
}
