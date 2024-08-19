import {Component, Input, OnInit, QueryList, ViewChildren} from '@angular/core';
import {MatLegacySlideToggle as MatSlideToggle, MatLegacySlideToggleChange as MatSlideToggleChange} from '@angular/material/legacy-slide-toggle';
import {Camera} from 'src/app/cameras/Camera';
import {ReportingComponent} from 'src/app/reporting/reporting.component';
import {ePresetOperations} from './preset-button/preset-button.component';
import {eMoveDirections} from './ptzbutton/ptzbutton.component';
import {UtilsService} from "../../shared/utils.service";
import {PTZPresetsInfoCommand, PTZPresetsInfoResponse, PTZService} from "../ptz.service";

@Component({
  selector: 'app-ptzcontrols',
  templateUrl: './ptzcontrols.component.html',
  styleUrls: ['./ptzcontrols.component.scss']
})
export class PTZControlsComponent implements OnInit {
  @Input() camera!: Camera;
  @Input() reporting!: ReportingComponent;
  @ViewChildren(MatSlideToggle) slideToggles!: QueryList<MatSlideToggle>;
  eMoveDirections: any = eMoveDirections;
  savePreset: boolean = false;
  clearPreset: boolean = false;
  isGuest: boolean = true;
  maxPresets: number = 0;
  presetsInfo!: PTZPresetsInfoResponse;

  constructor(private utils: UtilsService, private ptzService: PTZService) {
  }

  // ngFor counter for preset buttons
  counter(i: number) {
    return new Array(i);
  }

  presetsTooltip(presetNbr: number): string {
    return this.savePreset ? "Save the current view to preset " + presetNbr :
      this.clearPreset ? "Clear the saved position from preset " + presetNbr :
        "Move the view to the saved position in preset " + presetNbr;
  }

  presetSaveSwitchChanged($event: MatSlideToggleChange) {
    this.savePreset = $event.checked;
  }

  clearPresetSwitchChanged($event: MatSlideToggleChange) {
    this.clearPreset = $event.checked;
  }

  presetButtonPressed() {
    this.slideToggles.forEach(slideToggle => {
      slideToggle.checked = false;
    })
    this.clearPreset = this.savePreset = false;
  }

  presetFunctionDescription(): string {
    return this.savePreset ? "Click a preset button to save the current view to that preset " :
      this.clearPreset ? "Click a preset button to Clear the saved position from that preset " :
        "Click a preset button to move the camera to the saved position";
  }

  presetOperation(): ePresetOperations {
    return this.savePreset ? ePresetOperations.saveTo :
      this.clearPreset ? ePresetOperations.clearFrom :
        ePresetOperations.moveTo;
  }

  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
    this.ptzService.ptzPresetsInfo(new PTZPresetsInfoCommand(this.camera)).subscribe((result) => {
        this.presetsInfo = result;
        this.maxPresets = result.maxPresets > 32 ? 32 : this.presetsInfo.maxPresets;
      },
      reason => {
        this.reporting.errorMessage = reason;
      })
  }
}
