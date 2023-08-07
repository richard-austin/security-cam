import {Component, OnInit, ViewChild} from '@angular/core';
import {ReportingComponent} from '../reporting/reporting.component';
import {MatCheckbox, MatCheckboxChange} from '@angular/material/checkbox';
import {OnDestroy} from '@angular/core';
import {MatSelect} from '@angular/material/select/select';
import {timer} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {CurrentWifiConnection} from '../shared/current-wifi-connection';
import {WifiDetails} from '../shared/wifi-details';
import {WifiUtilsService} from '../shared/wifi-utils.service';
import {WifiConnectResult} from '../shared/wifi-connect-result';
import {IPDetails} from '../shared/IPDetails';

@Component({
  selector: 'app-wifi-settings',
  templateUrl: './wifi-settings.component.html',
  styleUrls: ['./wifi-settings.component.scss']
})
export class WifiSettingsComponent implements OnInit, OnDestroy {
  @ViewChild('selector') selector!: MatSelect;
  @ViewChild('wifiStatusCheckbox') wifiStatusCheckbox!: MatCheckbox;
  wifiEnabled: boolean = false;
  currentWifiConnection: CurrentWifiConnection = new CurrentWifiConnection();
  wifiList!: WifiDetails[];
  ethernetConnectionStatus: string = '';
  loading: boolean = true;
  needPassword: boolean = false;
  connecting: boolean = false;

  @ViewChild(ReportingComponent) reporting!: ReportingComponent;
  enterPasswordForm!: FormGroup;
  private password: string | undefined;
  isReady: boolean = false;

  constructor(private wifiUtilsService: WifiUtilsService) {
  }

  showWifi() {
    this.loading = true;
    this.wifiUtilsService.getCurrentWifiConnection().subscribe((result) => {
        this.currentWifiConnection = result;
        this.loading = false;
        this.selector.value = this.currentWifiConnection.accessPoint;
      },
      reason => {
        this.loading = false;
        this.reporting.errorMessage = reason;
      });
  }

  getLocalWifiDetails(): void {
    if (this.wifiEnabled) {
      this.wifiUtilsService.getLocalWifiDetails().subscribe((result) => {
          this.wifiList = result
            .filter(this.onlyUnique)
            .sort((a, b) => parseInt(b.signal) - parseInt(a.signal));
          this.showWifi();
        },
        reason => {
          this.reporting.errorMessage = reason;
        });
    }
  }

  setWifiStatus($event: MatCheckboxChange) {
    let status: string = $event.checked ? 'on' : 'off';
    this.loading = true;
    if (this.ethernetConnectionStatus === 'CONNECTED_VIA_ETHERNET') {
      this.wifiUtilsService.setWifiStatus(status).subscribe((result) => {
          this.wifiEnabled = result.status === 'on';
          if (this.wifiEnabled) {
            // Allow time for the Wi-Fi connection to re-establish so iwconfig can detect it
            timer(7000).subscribe(() => {
              this.getLocalWifiDetails();
              this.loading = false;
            });
          } else {
            this.wifiList = [];
            this.loading = false;
          }
        },
        reason => {
          this.wifiStatusCheckbox.checked = true;
          this.loading = false;
          this.reporting.errorMessage = reason;
        });
    } else {
      this.wifiStatusCheckbox.checked = true;
      this.loading = false;
    }
  }

  connect() {
    if (this.needPassword) {
      this.password = this.getFormControl(this.enterPasswordForm, 'password').value;
    } else {
      this.password = undefined;
    }
    this.connecting = true;
    this.needPassword = false;
    this.wifiUtilsService.setUpWifi(this.selector.value, this.password).subscribe((result) => {
        this.reporting.successMessage = JSON.parse(result.response)?.message;
        this.currentWifiConnection.accessPoint = this.selector.value;
        this.connecting = false;
      },
      (reason) => {
        this.connecting = false;
        let err: WifiConnectResult = reason.error;
        let response: any = JSON.parse(err.message);

        if (err.errorCode === 400) {
          if (response.returncode == 4) // nmcli return code 4: "Connection activation failed.",
          {
            this.needPassword = true;
            this.reporting.warningMessage = 'Please enter the password for ' + this.selector.value;
          }
          else if (response.returncode == 11)
            this.reporting.warningMessage = response.message;
        } else {
          this.reporting.errorMessage = new HttpErrorResponse({error: response.message});
        }
      });
  }

  onSelectorChange() {
    this.needPassword = false;
    this.reporting.dismiss();
  }

  cancelPasswordEntry() {
    this.needPassword = false;
    this.selector.value = this.currentWifiConnection.accessPoint;
    this.reporting.dismiss();
  }

  /**
   * onlyUnique: Show only one instance of each Wi-Fi access point name on the selector (maybe one for 2.4/5.0GHz etc)
   * @param value
   * @param index
   * @param self
   */
  onlyUnique(value: WifiDetails, index: number, self: WifiDetails[]) {
    let val: WifiDetails | undefined = self.find(a => a.ssid == value.ssid);
    if (val !== undefined && val.ssid !== '') {
      return self.indexOf(val) === index;
    }

    return false;
  }

  getFormControl(formGroup: FormGroup, fcName: string): FormControl {
    return formGroup.get(fcName) as FormControl;
  }

  ngOnInit(): void {
    this.isReady = false;
    this.wifiUtilsService.checkConnectedThroughEthernetNVR().subscribe(async (result) => {
        this.ethernetConnectionStatus = result.status;

        if (result.status !== 'NO_ETHERNET') {
          // We are overriding the result from the API call from here because that is intended for when the Cloud service is used
          this.ethernetConnectionStatus = 'NOT_CONNECTED_VIA_ETHERNET';
          try {
            let x: IPDetails[] = await this.wifiUtilsService.getActiveIPAddresses().toPromise();
            x.forEach((details: IPDetails) => {
              const idxSlash: number = details.ip.indexOf('/');
              const ip: string = details.ip.substring(0, idxSlash);
              if (details.cd.con_type === 'ethernet' && ip === window.location.hostname) {
                this.ethernetConnectionStatus = 'CONNECTED_VIA_ETHERNET';
              }
            });
            this.isReady = true;
          } catch (e: any) {
            this.reporting.errorMessage = e;
          }
        }
        this.isReady = true;
      },
      reason => {
        this.reporting.errorMessage = reason;
      });

    this.wifiUtilsService.checkWifiStatus().subscribe((result) => {

        this.wifiEnabled = result.status === 'on';
        if (this.wifiEnabled) {
          this.getLocalWifiDetails();
        } else {
          this.loading = false;
        }
      },
      reason => {
        this.reporting.errorMessage = reason;
      });

    this.enterPasswordForm = new FormGroup({
      password: new FormControl(this.password, [Validators.required, Validators.maxLength(35)]),
    }, {updateOn: 'change'});
  }

  ngOnDestroy(): void {
  }
}
