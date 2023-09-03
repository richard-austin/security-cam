import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import {ReportingComponent} from "../../reporting/reporting.component";
import {Camera} from "../../cameras/Camera";

/**
 * isValidDeviceIP: Custom validator to check that the onvif URL presented for getting camera details does not have
 *                  the same ip:port as any already in the camera list.
 *                  Also checks that the ip:port is valid IP4.
 * @param cameras
 */
export function isValidDeviceIP(cameras: Map<string, Camera>): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    let cams = cameras;
    let retVal: boolean = false;
    let badHost: boolean = true;
    try {
      let url: URL = new URL(control.value);
      badHost = !/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+$/.test(url.host);
      cams.forEach((cam) => {
        if(cam.onvifHost === url.host)
          retVal = true
      });
    }
    catch(ex) {
      badHost = true;
    }

    return retVal || badHost ? {onvifUrl: retVal, badHost: badHost} : null;
  }
}

@Component({
  selector: 'app-add-as-onvif-device',
  templateUrl: './add-as-onvif-device.component.html',
  styleUrls: ['./add-as-onvif-device.component.scss']
})
export class AddAsOnvifDeviceComponent implements OnInit {
  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent
  @Input() cameras!: Map<string, Camera>;
  @Output() startFindCameraDetails: EventEmitter<string> = new EventEmitter<string>();

  constructor() { }
  onvifUrl: string = 'http://192.168.1.1:8080/onvif/device_service';
  addCameraForm!: FormGroup;

  startCapabilitiesDiscovery() {
    this.startFindCameraDetails.emit(this.onvifUrl);
    this.hideAddCameraDialogue();
  }

  hideAddCameraDialogue() {
    this.hideDialogue.emit();
  }

  getFormControl(fcName: string) {
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
      onvifUrl: new FormControl(this.onvifUrl, [Validators.required, Validators.maxLength(60), Validators.pattern("^(http:\\/\\/)[\\w.-]+(?:\\.[\\w\\.-]+)+[\\w\\-\\._~:/?#[\\]@!\\$&'\\(\\)\\*\\+,;=.]+$"), isValidDeviceIP(this.cameras)]),
    }, {updateOn: "change"});

    // Ensure camera form controls highlight immediately if invalid
    this.addCameraForm.markAllAsTouched();
  }
}
