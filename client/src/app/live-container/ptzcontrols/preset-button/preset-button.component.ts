import {input, Input, InputSignal} from '@angular/core';
import {Component, OnInit} from '@angular/core';
import {Preset, PTZPresetCommand, PTZService} from "../../ptz.service";
import {Camera} from "../../../cameras/Camera";
import {ReportingComponent} from "../../../reporting/reporting.component";
import {MatMiniFabButton} from "@angular/material/button";

export enum ePresetOperations {moveTo, saveTo, clearFrom}

@Component({
  selector: 'app-preset-button',
  templateUrl: './preset-button.component.html',
  imports: [
    MatMiniFabButton
  ],
  styleUrls: ['./preset-button.component.scss']
})
export class PresetButtonComponent implements OnInit {
  camera: InputSignal<Camera> = input.required<Camera>();
  reporting: InputSignal<ReportingComponent> = input.required<ReportingComponent>();
  presetInfo: InputSignal<Preset> = input.required<Preset>();
  presetNumber: InputSignal<string> = input.required<string>();
  operation: InputSignal<ePresetOperations> = input.required<ePresetOperations>();
  isGuest: InputSignal<boolean> = input<boolean>(true);

  constructor(private ptz: PTZService) {
  }

  preset() {
    let ptz: PTZPresetCommand = new PTZPresetCommand(this.operation(), this.camera(), this.presetInfo().token)
    this.ptz.preset(ptz).subscribe({
      next: () => {
      },
      error: reason => {
        this.reporting().errorMessage = reason;
      }
    });
  }

  presetPressed($event: MouseEvent | TouchEvent) {
    if ($event.type === 'touchstart') {
      $event.preventDefault();
    }
    this.preset();
  }

  ngOnInit(): void {
  }
}
