import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import { Camera } from 'src/app/cameras/Camera';
import {CameraService} from "../../cameras/camera.service";
import {ReportingComponent} from "../../reporting/reporting.component";
import {Encryption} from "./encryption";

export class CameraAdminCredentials
{
  userName: string='';
  password: string='';
}
@Component({
  selector: 'app-credentials-for-camera-access',
  templateUrl: './credentials-for-camera-access.component.html',
  styleUrls: ['./credentials-for-camera-access.component.scss']
})
export class CredentialsForCameraAccessComponent implements OnInit {

  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent
  @Input() camera!: Camera;

  constructor(private camSvc:CameraService) { }

  username: string = '';
  password: string = '';
  setPasswordForm!: FormGroup;
   hidePasswordDialogue() {
    this.hideDialogue.emit();
  }

  async updateCredentials() {
    this.username = this.getFormControl('cameraUserName').value;
    this.password = this.getFormControl('cameraPassword').value;

    let creds: CameraAdminCredentials = new CameraAdminCredentials();
    creds.password = this.password;
    creds.userName = this.username;
    const jsonCreds = JSON.stringify(creds);
    const crypto: Encryption = new Encryption();
    this.camera.cred = await crypto.encrypt(jsonCreds);
    this.hidePasswordDialogue();
  }

  getFormControl(fcName: string): FormControl {
    return this.setPasswordForm.get(fcName) as FormControl;
  }

  anyInvalid(): boolean
  {
    return this.setPasswordForm.invalid;
  }

  ngOnInit(): void {
    this.setPasswordForm = new FormGroup({
      cameraUserName: new FormControl(this.username, [Validators.required, Validators.maxLength(20), Validators.pattern("^[a-zA-Z0-9](_(?!(\.|_))|\.(?!(_|\.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$")]),
      cameraPassword: new FormControl(this.password, [Validators.required, Validators.maxLength(25), Validators.pattern("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$")])
    }, {updateOn: "change"});

    // Ensure camera form controls highlight immediately if invalid
    this.setPasswordForm.markAllAsTouched();
  }
}
