import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {ReportingComponent} from "../../reporting/reporting.component";

@Component({
  selector: 'app-add-as-onvif-device',
  templateUrl: './add-as-onvif-device.component.html',
  styleUrls: ['./add-as-onvif-device.component.scss']
})
export class AddAsOnvifDeviceComponent implements OnInit {
  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent
  @Output() startFindCameraDetails: EventEmitter<string> = new EventEmitter<string>();

  constructor() { }
  onvifUrl: string = 'http://192.168.1.43:8000/onvif/device_service';
  addCameraForm!: FormGroup;

  startCapabilitiesDiscovery() {
    this.startFindCameraDetails.emit(this.onvifUrl);
  }

  hideAddCameraDialogue() {
    this.hideDialogue.emit();
  }

  getFormControl(fcName: string): FormControl {
    return this.addCameraForm.get(fcName) as FormControl;
  }
  updateField() {
    let control: FormControl = this.getFormControl("onvifUrl")
    this.onvifUrl = control.value;
  }

  anyInvalid(): boolean
  {
    return this.addCameraForm.invalid;
  }

  ngOnInit(): void {
    this.addCameraForm = new FormGroup({
      onvifUrl: new FormControl(this.onvifUrl, [Validators.required, Validators.maxLength(60), Validators.pattern("^(http:\\/\\/)[\\w.-]+(?:\\.[\\w\\.-]+)+[\\w\\-\\._~:/?#[\\]@!\\$&'\\(\\)\\*\\+,;=.]+$")]),
    }, {updateOn: "change"});

    // Ensure camera form controls highlight immediately if invalid
    this.addCameraForm.markAllAsTouched();
  }
}
