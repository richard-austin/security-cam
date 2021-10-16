import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {CameraService} from '../cameras/camera.service';
import {Camera} from "../cameras/Camera";
import { ReportingComponent } from '../reporting/reporting.component';
import {animate, state, style, transition, trigger} from '@angular/animations';

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
  cameras:Map<string, Camera> = new Map<string, Camera>();
  displayedColumns = ['name', 'address', 'controlUri'];
  expandedElement!:Camera | null; //: PeriodicElement | null;
  streamColumns = ['descr', 'netcam_uri', 'uri', 'nms_uri', 'video_width', 'video_height'];

  constructor(private cameraSvc: CameraService) {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    // Set up the available streams/cameras for selection by the check boxes
    this.cameraSvc.loadCameras().subscribe(cameras => {
        this.cameras  = cameras;
        this.downloading = false;
      },
      reason => this.errorReporting.errorMessage = reason);
  }

  toggle(el: Camera) {
    this.expandedElement = this.expandedElement === el ? null : el;
  }
}
