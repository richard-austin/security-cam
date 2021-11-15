import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {CameraService} from "../cameras/camera.service";
import {ReportingComponent} from "../reporting/reporting.component";

export class CameraAdminCredentials
{
  camerasAdminUserName: string='';
  camerasAdminPassword: string='';
}

@Component({
  selector: 'app-credentials-for-camera-access',
  templateUrl: './credentials-for-camera-access.component.html',
  styleUrls: ['./credentials-for-camera-access.component.scss']
})
export class CredentialsForCameraAccessComponent implements OnInit {

  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  @Input() reporting!: ReportingComponent

  constructor(private camSvc:CameraService) { }

  camerasUsername: string = '';
  camerasPassword: string = '';
  setPasswordForm!: FormGroup;


  hidePasswordDialogue() {
    this.hideDialogue.emit();
  }

  updateCredentials() {
    this.camerasUsername = this.getFormControl('camerasUsername').value;
    this.camerasPassword = this.getFormControl('camerasPassword').value;

    let creds: CameraAdminCredentials = new CameraAdminCredentials();
    creds.camerasAdminPassword = this.camerasPassword;
    creds.camerasAdminUserName = this.camerasUsername;
    this.camSvc.setCameraAdminCredentials(creds).subscribe(() =>{
        this.hidePasswordDialogue();
        this.reporting.successMessage="Camera Access Credentials Updated";
    },
      (reason) => {
        this.reporting.errorMessage = reason;
      })
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
      camerasUsername: new FormControl(this.camerasUsername, [Validators.required, Validators.maxLength(20), Validators.pattern("^[a-zA-Z0-9](_(?!(\.|_))|\.(?!(_|\.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$")]),
      camerasPassword: new FormControl(this.camerasPassword, [Validators.required, Validators.maxLength(25), Validators.pattern("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$")])
    }, {updateOn: "change"});

    // Ensure camera form controls highlight immediately if invalid
    this.setPasswordForm.markAllAsTouched();
  }
}
