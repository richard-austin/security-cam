import { Input } from '@angular/core';
import { Component, OnInit } from '@angular/core';
import {Preset, PTZPresetCommand, PTZService} from "../../ptz.service";
import {Camera} from "../../../cameras/Camera";
import {ReportingComponent} from "../../../reporting/reporting.component";

export enum ePresetOperations {moveTo, saveTo, clearFrom}

@Component({
    selector: 'app-preset-button',
    templateUrl: './preset-button.component.html',
    styleUrls: ['./preset-button.component.scss'],
    standalone: false
})
export class PresetButtonComponent implements OnInit {
  @Input() camera!: Camera;
  @Input() reporting!: ReportingComponent;
  @Input() presetInfo!: Preset;
  @Input() presetNumber!: string;
  @Input() operation!: ePresetOperations;
  @Input() color!: string;
  @Input() isGuest: boolean = true;

  constructor(private ptz: PTZService) { }

  preset() {
    let ptz: PTZPresetCommand = new PTZPresetCommand(this.operation, this.camera, this.presetInfo.token)
    this.ptz.preset(ptz).subscribe(() => {
      },
      reason => {
        this.reporting.errorMessage = reason;
      })
  }

  presetPressed() {
    this.preset();
  }

  ngOnInit(): void {
  }
}
