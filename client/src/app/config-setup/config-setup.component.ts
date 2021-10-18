import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
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
  ],
})
export class ConfigSetupComponent implements OnInit, AfterViewInit {
  @ViewChild('errorReporting') errorReporting!: ReportingComponent;
  downloading: boolean = true;
  cameras: Map<string, Camera> = new Map<string, Camera>();
  displayedColumns = ['name', 'address', 'controlUri'];
  expandedElement!: Camera | null; //: PeriodicElement | null;
  streamColumns = ['descr', 'netcam_uri', 'uri', 'nms_uri', 'video_width', 'video_height'];
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

  ngOnInit(): void {
    // Set up the available streams/cameras for selection by the check boxes
    this.cameraSvc.loadCameras().subscribe(cameras => {
        this.cameras = cameras;
        this.downloading = false;
        this.list$ = new BehaviorSubject<Camera[]>(Array.from(this.cameras.values()));

        let index: number = 0;
        const toCameraGroups = this.list$.value.map(camera => {
          let list$: BehaviorSubject<Stream[]> = new BehaviorSubject<Stream[]>(Array.from(camera.streams.values()));
          const toStreamGroups = list$.value.map((stream: Stream) => {
            return new FormGroup({
              descr: new FormControl(stream.descr, [Validators.required, Validators.maxLength(25)]),
            }, {updateOn: "blur"});
          });
          this.streamControls[index++] = new FormArray(toStreamGroups);

          return new FormGroup({
            name: new FormControl(camera.name, [Validators.required, Validators.maxLength(25)]),
          }, {updateOn: "blur"});
        });

        this.controls = new FormArray(toCameraGroups)
      },
      reason => this.errorReporting.errorMessage = reason);
  }

  ngAfterViewInit(): void {
  }

  toggle(el: {key: string, value:Camera}) {
    this.expandedElement = this.expandedElement === el.value ? null : el.value;
  }
}
