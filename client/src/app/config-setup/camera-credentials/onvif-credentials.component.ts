import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {Camera} from 'src/app/cameras/Camera';
import {CameraService, OnvifCredentials} from "../../cameras/camera.service";
import {ReportingComponent} from "../../reporting/reporting.component";
import {Encryption} from "./encryption";
import {SharedModule} from "../../shared/shared.module";
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";

@Component({
  selector: 'app-camera-credentials',
  templateUrl: './onvif-credentials.component.html',
  styleUrls: ['./onvif-credentials.component.scss'],
  imports: [
      SharedModule,
      SharedAngularMaterialModule
  ],
  standalone: true
})
export class OnvifCredentialsComponent implements OnInit, AfterViewInit {

  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Output() haveOnvifCredentials: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent
  @Input() camera!: Camera | undefined | null;

  constructor(private camSvc: CameraService) {
  }

  username: string = '';
  password: string = '';
  setPasswordForm!: UntypedFormGroup;
  usernameTooltip: string = "Enter the user name for administrative access to the cameras. Note this is to give this application access to the cameras not to set it on the cameras.";
  passwordTooltip: string = "Enter the password for administrative access to the cameras. Note this is to give this application access to the cameras not to set it on the cameras.";
  hidePasswordDialogue() {
    this.hideDialogue.emit();
  }

  async updateCredentials() {
    this.username = this.getFormControl('cameraUserName').value;
    this.password = this.getFormControl('cameraPassword').value;
    if (this.camera !== null && this.camera !== undefined) {
      let creds: OnvifCredentials = new OnvifCredentials();
      creds.password = this.password;
      creds.userName = this.username;
      const jsonCreds = JSON.stringify(creds);
      const crypto: Encryption = new Encryption(this.camSvc.publicKey);
      this.camera.cred = await crypto.encrypt(jsonCreds);
      this.hidePasswordDialogue();
    } else {
      const creds: OnvifCredentials = new OnvifCredentials();
      creds.userName = this.username;
      creds.password = this.password;
      this.camSvc.setOnvifCredentials(creds).subscribe(() => {
        this.haveOnvifCredentials.emit();
      }, reason => {
        this.reporting.errorMessage = reason;
      });
      this.hidePasswordDialogue();
    }
  }

  getFormControl(fcName: string): UntypedFormControl {
    return this.setPasswordForm.get(fcName) as UntypedFormControl;
  }

  anyInvalid(): boolean {
    return this.setPasswordForm.invalid;
  }

  ngOnInit(): void {
    this.setPasswordForm = new UntypedFormGroup({
      cameraUserName: new UntypedFormControl(this.username, [Validators.required, Validators.maxLength(20), Validators.pattern("^[a-zA-Z0-9](_(?!(\.|_))|\.(?!(_|\.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$")]),
      cameraPassword: new UntypedFormControl(this.password, [Validators.required, Validators.maxLength(25), Validators.pattern("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$")])
    }, {updateOn: "change"});

    // Ensure camera form controls highlight immediately if invalid
    this.setPasswordForm.markAllAsTouched();
  }
  ngAfterViewInit(): void {
    if (this.camera === null || this.camera === undefined) {
      this.usernameTooltip = "Enter the user name for Onvif authentication. This is used on overall Onvif discovery";
      this.passwordTooltip = "Enter the password for Onvif authentication.";
    }
  }
}
