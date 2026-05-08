import {
  AfterViewChecked,
  Component,
  ElementRef,
  inject,
  OnInit,
  Signal,
  signal,
  TrackByFunction,
  viewChild
} from '@angular/core';
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {UntypedFormArray, UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {BehaviorSubject} from "rxjs";
import {Device, UtilsService} from "../shared/utils.service";
import {HttpErrorResponse} from "@angular/common/http";
import {ReportingComponent} from "../reporting/reporting.component";
import {SharedModule} from '../shared/shared.module';
import {RowDeleteConfirmComponent} from "../config-setup/row-delete-confirm/row-delete-confirm.component";

declare let objectHash: (obj: Object) => string;

@Component({
  selector: 'app-ad-hoc-hosting-config',
  imports: [SharedAngularMaterialModule, SharedModule, RowDeleteConfirmComponent, RowDeleteConfirmComponent],
  templateUrl: './ad-hoc-hosting-config.component.html',
  styleUrl: './ad-hoc-hosting-config.component.scss',
})
export class AdHocHostingConfigComponent implements OnInit, AfterViewChecked {
  columns: string[] = ['delete', 'devicename', 'ipaddress', 'ipport'];
  footerColumns = ['buttons'];
  devices!: Device[];
  isGuest: boolean = true;
  updating: boolean = false;
  confirmSave: boolean = false;
  confirmRestore: boolean = false;
  confirmNew: boolean = false;
  savedDataHash: string = "";

  tableForms!: UntypedFormArray
  showDeviceDeleteConfirm: number = -1;
  reporting: Signal<ReportingComponent> = viewChild.required('errorReporting');
  animationEnter = signal('enter-animation');
  animationLeave = signal('leaving-animation');
  scrollableContent = viewChild.required<ElementRef<HTMLDivElement>>('scrollableContent');
  private utils = inject(UtilsService);

  downloading: boolean = true;

  constructor() {
    this.devices = [];
  }

  setScrollWindow() {
    const sc = this.scrollableContent().nativeElement;
    sc.style = this.utils.getScrollableContentStyle(sc, true);
  }

  protected trackBy: TrackByFunction<Device> = (index: number, dev: Device) =>  dev.id;

  dataHasChanged(): boolean {
    return this.devices && objectHash(this.devices) !== this.savedDataHash;
  }

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
    return !this.tableForms || this.tableForms.invalid;
  }

  setUpTableFormControls() {
    let list = new BehaviorSubject<Device[]>(this.devices);
    let formGroups  = list.value.map((device: Device) => {
      return new UntypedFormGroup({
        name: new UntypedFormControl({
          value: device.name,
          disabled: false
        },[Validators.required, Validators.maxLength(25)]),
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
      // Renumber the indices
      this.devices.forEach((device: Device, index: number) => {
        device.id = index;
      });
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
    const dev = new Device();
    dev.id = this.devices.length;
    this.devices.push(dev);
    this.devices = [...this.devices]; // Needs to be a new array for the table to reflect the change
    this.setUpTableFormControls();
  }

  commitConfig() {
    this.utils.updateAdhocDeviceList(JSON.stringify(this.devices)).subscribe({
        next: () => {
          this.reporting().successMessage = "Update ad hoc device list Successful!";
          this.updating = false;
          // Update the saved data hash
          this.savedDataHash = objectHash(this.devices);
        },
        error: reason => {
          this.reporting().errorMessage = reason
          this.updating = false;
        }
      });
  }

  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
   this.utils.loadAdHocDevices().subscribe(  {
      next: (devices: Device[]) => {
        devices.forEach((device: Device, index: number) => {
          device.id = index;
        });
        this.devices = devices;
        this.setUpTableFormControls();
        this.downloading = false;
        this.savedDataHash = objectHash(this.devices);
      },
      error: () => {
        this.devices = new Array<Device>();
        this.devices.push(new Device());
        this.setUpTableFormControls();
        this.reporting().errorMessage = new HttpErrorResponse({error: 'The configuration file is absent, empty or corrupt. Please set up the configuration for your ad hoc devices and save it.'});
        this.downloading = false;
      }
    });
  }

  ngAfterViewChecked(): void {
    this.setScrollWindow();
  }

  protected readonly UtilsService = UtilsService;
}
