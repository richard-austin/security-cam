import {Component, OnInit} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from '@angular/forms';

export class SMTPData {
  auth!: boolean
  username!: string;
  password!: string;
  enableStartTLS!: boolean;
  sslProtocols!: string;
  sslEnabled!: boolean;
  sslTrust!: string;
  host!: string;
  port!: number
  fromAddress!: string;
}

@Component({
  selector: 'app-setup-smtpclient',
  templateUrl: './setup-smtpclient.component.html',
  styleUrls: ['./setup-smtpclient.component.scss']
})
export class SetupSMTPClientComponent implements OnInit {
  setupSMTPForm!: FormGroup;
  smtpData: SMTPData = new SMTPData();
  confirmPassword!: string;
   passwordMatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.confirmPassword = control.value;
      const ok = this.smtpData.password !== control.value;
      return ok ? {notMatching: {value: control.value}} : null;
    };
  }
  emailValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.smtpData.fromAddress= control.value;
      // Update the validation status of the confirmPassword field
      // if (this.confirmPassword !== "") {
      //   let cpControl: AbstractControl | null = this.setupSMTPForm.get("confirmEmail");
      //   cpControl?.updateValueAndValidity();
      // }

      const ok = !new RegExp("^([a-zA-Z0-9_\\-.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(]?)$").test(control.value);
      return ok ? {pattern: {value: control.value}} : null;
    };
  }

  getFormControl(fcName: string): FormControl {
    return this.setupSMTPForm.get(fcName) as FormControl;
  }

  anyInvalid(): boolean {
    return this.setupSMTPForm.invalid;
  }

  confirmOnReturn($event: InputEvent) {
    // Ensure password field is up-to-date for the confirmPassword validity check
    this.smtpData.password = this.getFormControl('password').value;

    // if ($event.inputType == 'insertLineBreak' && !this.anyInvalid())
    //   if (!this.anyInvalid())
    //     this.register();
  }


  ngOnInit(): void {
    this.setupSMTPForm = new FormGroup({
      auth: new FormControl(this.smtpData.auth,[Validators.required]),
      username: new FormControl(this.smtpData.username, [Validators.required, Validators.maxLength(50)]),
      password: new FormControl(this.smtpData.password, [Validators.required, Validators.maxLength(35)]),
      confirmPassword: new FormControl(this.confirmPassword, [Validators.required, Validators.maxLength(35), this.passwordMatchValidator()]),
      enableStartTLS: new FormControl(this.smtpData.enableStartTLS, [Validators.required]),
      sslProtocols: new FormControl(this.smtpData.sslProtocols, [Validators.required, Validators.maxLength(40)]),
      sslTrust: new FormControl(this.smtpData.sslTrust, [Validators.required]),
      host: new FormControl(this.smtpData.host, [Validators.required, Validators.minLength(3), Validators.maxLength(50)]),
      port: new FormControl(this.smtpData.port, [Validators.required, Validators.max(65535), Validators.min(1)]),
      fromAddress: new FormControl(this.smtpData.fromAddress, [Validators.required, Validators.maxLength(40), this.emailValidator()])
    }, {updateOn: "change"});
    // Ensure camera form controls highlight immediately if invalid
    this.setupSMTPForm.markAllAsTouched();

  }
}
