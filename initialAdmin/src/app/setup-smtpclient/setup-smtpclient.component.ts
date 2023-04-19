import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from '@angular/forms';
import {MatCheckboxChange} from "@angular/material/checkbox";
import {ReportingComponent} from "../reporting/reporting.component";

export class SMTPData {
  auth: boolean = true;
  username!: string;
  password!: string;
  enableStartTLS: boolean = true;
  sslProtocols: string = "TLSv1.2";
  sslEnabled!: boolean;
  sslTrust!: string;
  host!: string;
  port: number = 587;
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
  error: boolean = false;

  @ViewChild(ReportingComponent) reporting: ReportingComponent = new ReportingComponent();

  passwordMatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.confirmPassword = control.value;
      const ok = this.smtpData.password !== control.value;
      return ok ? {notMatching: {value: control.value}} : null;
    };
  }

  emailValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.smtpData.fromAddress = control.value;
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
    let retVal: boolean = false;
    Object.keys(this.setupSMTPForm.controls).forEach(key => {
      let ctl: FormControl = this.getFormControl(key);
      if (ctl.enabled) {
        retVal ||= ctl.invalid;
      }
    });
    return retVal;
  }

  confirm() {

  }


  confirmOnReturn($event: InputEvent) {
    // Ensure password field is up-to-date for the confirmPassword validity check
    this.smtpData.password = this.getFormControl('password').value;

    // if ($event.inputType == 'insertLineBreak' && !this.anyInvalid())
    //   if (!this.anyInvalid())
    //     this.register();
  }

  hideSetupForm() {
    window.location.href = '#/';
  }

  updateAuthState($event: MatCheckboxChange) {
    this.smtpData.auth = $event.checked;
    let un: FormControl = this.getFormControl('username');
    let pw: FormControl = this.getFormControl('password');
    let cp: FormControl = this.getFormControl('confirmPassword');

    if (this.smtpData.auth) {
      un.enable({onlySelf: true, emitEvent: false});
      pw.enable({onlySelf: true, emitEvent: false});
      cp.enable({onlySelf: true, emitEvent: false});
    } else {
      un.disable({onlySelf: true, emitEvent: false});
      pw.disable({onlySelf: true, emitEvent: false});
      cp.disable({onlySelf: true, emitEvent: false});
    }
  }

  updateStartTLSState($event: MatCheckboxChange) {
    this.smtpData.enableStartTLS = $event.checked;
    let sp: FormControl = this.getFormControl('sslProtocols');
    let st: FormControl = this.getFormControl('sslTrust');

    if (this.smtpData.enableStartTLS) {
      sp.enable({onlySelf: true, emitEvent: false});
      st.enable({onlySelf: true, emitEvent: false});
    } else {
      sp.disable({onlySelf: true, emitEvent: false});
      st.disable({onlySelf: true, emitEvent: false});
    }
  }

  ngOnInit(): void {
    this.setupSMTPForm = new FormGroup({
      auth: new FormControl(this.smtpData.auth, [Validators.required]),
      username: new FormControl({
        value: this.smtpData.username,
        disabled: !this.smtpData.auth
      }, [Validators.required, Validators.maxLength(50)]),
      password: new FormControl(this.smtpData.password, [Validators.required, Validators.maxLength(50)]),
      confirmPassword: new FormControl(this.confirmPassword, [Validators.required,Validators.maxLength(50), this.passwordMatchValidator()]),
      enableStartTLS: new FormControl(this.smtpData.enableStartTLS, [Validators.required]),
      sslProtocols: new FormControl(this.smtpData.sslProtocols, [Validators.required]),
      sslTrust: new FormControl(this.smtpData.sslTrust, [Validators.required]),
      host: new FormControl(this.smtpData.host, [Validators.required, Validators.minLength(3), Validators.maxLength(50)]),
      port: new FormControl(this.smtpData.port, [Validators.required, Validators.max(65535), Validators.min(1)]),
      fromAddress: new FormControl(this.smtpData.fromAddress, [Validators.required, Validators.maxLength(40), this.emailValidator()])
    }, {updateOn: "change"});
    // Ensure camera form controls highlight immediately if invalid
    this.setupSMTPForm.markAllAsTouched();
  }
}
