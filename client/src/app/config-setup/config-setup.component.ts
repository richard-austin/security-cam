import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera} from "../cameras/Camera";
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
  list$!: BehaviorSubject<Camera[]>;

  constructor(private cameraSvc: CameraService) {
  }

  getControl(index: number, fieldName: string): FormControl {
    return this.controls.at(index).get(fieldName) as FormControl;
  }

  update(index: number, field: string, value: any) {
    console.log(index, field, value);

    Array.from(this.cameras.values()).forEach((cam: Camera, i) => {
      if (i === index) { // @ts-ignore
        cam[field] = value;
      }
    });

    let y = this.cameras;
  }

  updateField(index: number, field: string) {
    const control = this.getControl(index, field);
    if (control.valid) {
      this.update(index, field, control.value);
    }
  }

  ngOnInit(): void {
    // Set up the available streams/cameras for selection by the check boxes
    this.cameraSvc.loadCameras().subscribe(cameras => {
        this.cameras = cameras;
        this.downloading = false;
        this.list$ = new BehaviorSubject<Camera[]>(Array.from(this.cameras.values()));

        const toGroups = this.list$.value.map(entity => {
          return new FormGroup({
            name: new FormControl(entity.name, [Validators.required, Validators.maxLength(25)]),
          }, {updateOn: "blur"});
        });

        this.controls = new FormArray(toGroups)
      },
      reason => this.errorReporting.errorMessage = reason);
  }

  ngAfterViewInit(): void {
  }

  toggle(el: Camera) {
    this.expandedElement = this.expandedElement === el ? null : el;
  }
}
