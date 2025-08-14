import {Component, OnInit} from '@angular/core';
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {FormControl, UntypedFormArray, UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {BehaviorSubject} from "rxjs";

class Device  {
  name!: string;
  ipAddress!: string;
  ipPort!: number;
}

@Component({
  selector: 'app-ad-hoc-hosting-config',
  imports: [SharedAngularMaterialModule],
  templateUrl: './ad-hoc-hosting-config.component.html',
  styleUrl: './ad-hoc-hosting-config.component.scss'
})
export class AdHocHostingConfigComponent implements OnInit {
  columns: string[] = ['devicename', 'ipaddress', 'ipport'];
  devices: Device[] = [{name: 'Front Room Switch', ipAddress:'192.168.1.253', ipPort:80}, {name: 'Hall Switch', ipAddress:'192.168.1.232', ipPort:80}];

  tableForms!: UntypedFormArray

  getControl(index: number, fieldName: string): UntypedFormControl {
    if(this.tableForms) {
      return this.tableForms.at(index).get(fieldName) as UntypedFormControl;
    }
    return new UntypedFormControl(null, [Validators.required]);
  }

  updateField(index: number, field: string) {
    const control = this.getControl(index, field);
    if (control) {
      this.updateDevice(index, field, control.value);
    }
  }

  updateDevice(index: number, field: string, value: any) {
    Array.from(this.devices).forEach((dev: Device, i) => {
      if (i === index) { // @ts-ignore
        dev[field] = value;
      }
    });
  }

  setUpTableFormControls() {
    let list = new BehaviorSubject<Device[]>(this.devices);
    let formGroups  = list.value.map((device: Device) => {
      return new UntypedFormGroup({
        name: new UntypedFormControl(device.name, [Validators.required, Validators.maxLength(25)]),
        ipAddress: new UntypedFormControl({
          value: device.ipAddress,
          disabled: false
        }, [Validators.pattern(/\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}\b/)]),
        ipPort: new UntypedFormControl({
          value: device.ipPort,
          disabled: false
        }, [Validators.max(65535), Validators.min(80), Validators.maxLength(5), Validators.required])
      });
    });
    this.tableForms = new UntypedFormArray(formGroups);
    // Ensure device form controls highlight immediately if invalid
    for (let i = 0; i < this.tableForms.length; ++i) {
      this.tableForms.at(i).markAllAsTouched();
    }


  }

  ngOnInit(): void {
    this.setUpTableFormControls();
  }
}
