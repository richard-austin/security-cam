import {Component, Input, OnInit} from '@angular/core';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import {Camera} from 'src/app/cameras/Camera';
import { ReportingComponent } from 'src/app/reporting/reporting.component';
import { eMoveDirections } from './ptzbutton/ptzbutton.component';

@Component({
  selector: 'app-ptzcontrols',
  templateUrl: './ptzcontrols.component.html',
  styleUrls: ['./ptzcontrols.component.scss']
})
export class PTZControlsComponent implements OnInit {
  @Input() camera!:Camera | null;
  @Input() reporting!: ReportingComponent;
  eMoveDirections: any = eMoveDirections;
  savePreset: boolean = false;
  constructor() { }

  // ngFor counter for preset buttons
  counter(i: number) {
    return new Array(i);
  }

  ngOnInit(): void {
  }

  presetSaveSwitchChanged($event: MatSlideToggleChange) {
    this.savePreset = $event.checked;
  }

  clearPresetSwitchChanged($event: MatSlideToggleChange) {

  }
}
