import { Input } from '@angular/core';
import { Component, OnInit } from '@angular/core';
import {PTZPreset, PTZService} from "../../ptz.service";
import {Camera} from "../../../cameras/Camera";
import {ReportingComponent} from "../../../reporting/reporting.component";

export enum ePresetOperations {moveTo, saveTo, clearFrom}

@Component({
  selector: 'app-preset-button',
  templateUrl: './preset-button.component.html',
  styleUrls: ['./preset-button.component.scss']
})
export class PresetButtonComponent implements OnInit {
  @Input() camera!: Camera | null;
  @Input() reporting!: ReportingComponent;
  @Input() presetId!: string;
  @Input() operation!: ePresetOperations;
  @Input() color!: string;
  @Input() isGuest: boolean = true;

  constructor(private ptz: PTZService) { }

  preset() {
    let ptz: PTZPreset = new PTZPreset(this.operation, this.camera?.onvifHost as string, this.presetId)
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
