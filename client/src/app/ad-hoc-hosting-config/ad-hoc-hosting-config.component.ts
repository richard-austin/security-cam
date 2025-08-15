import {Component, OnInit} from '@angular/core';
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {UntypedFormArray, UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {BehaviorSubject} from "rxjs";
import {RowDeleteConfirmComponent} from "../config-setup/row-delete-confirm/row-delete-confirm.component";
import {animate, state, style, transition, trigger} from "@angular/animations";

class Device  {
  name!: string;
  ipAddress!: string;
  ipPort!: number;
}

@Component({
  selector: 'app-ad-hoc-hosting-config',
  imports: [SharedAngularMaterialModule, RowDeleteConfirmComponent],
  templateUrl: './ad-hoc-hosting-config.component.html',
  styleUrl: './ad-hoc-hosting-config.component.scss',
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ])
  ],
})
export class AdHocHostingConfigComponent implements OnInit {
  columns: string[] = ['delete', 'devicename', 'ipaddress', 'ipport'];
  footerColumns = ['buttons'];
  devices!: Device[];

  tableForms!: UntypedFormArray
  showDeviceDeleteConfirm: number = -1;

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

  anyInvalid(): boolean {
    return this.tableForms.invalid;
  }

  setUpTableFormControls() {
    let list = new BehaviorSubject<Device[]>(this.devices);
    let formGroups  = list.value.map((device: Device) => {
      return new UntypedFormGroup({
        name: new UntypedFormControl(device.name, [Validators.required, Validators.maxLength(25)]),
        ipAddress: new UntypedFormControl({
          value: device.ipAddress,
          disabled: false
        }, [Validators.required, Validators.pattern(/\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}\b/)]),
        ipPort: new UntypedFormControl({
          value: device.ipPort,
          disabled: false
        }, [Validators.max(65535), Validators.min(80), Validators.required])
      });
    });
    this.tableForms = new UntypedFormArray(formGroups);
    // Ensure device form controls highlight immediately if invalid
    for (let i = 0; i < this.tableForms.length; ++i) {
      this.tableForms.at(i).markAllAsTouched();
    }
  }

  deleteDevice(i: number) {
    if(i >= 0 && i < this.tableForms.length) {
      this.devices.splice(i, 1);
      this.devices = [...this.devices];  // Needs to be a new array for the table to reflect the change
      this.setUpTableFormControls();
    } else {
      console.log("delete index "+i+" is out of range")
    }
  }

  toggleDeviceDeleteConfirm(index: number) {
    // this.allOff();
    this.showDeviceDeleteConfirm = this.showDeviceDeleteConfirm !== index ? index : -1;
  }

   getDeviceDeleteDisabledState(device: Device) {
    return false;
  }

  addDevice() {
    this.devices.push(new Device());
    this.devices = [...this.devices];
    this.setUpTableFormControls();
  }

  ngOnInit(): void {
    this.devices =  [{name: 'Front Room Switch', ipAddress:'192.168.1.253', ipPort:80}, {name: 'Hall Switch', ipAddress:'192.168.1.232', ipPort:80}];
    this.setUpTableFormControls();
  }
}
