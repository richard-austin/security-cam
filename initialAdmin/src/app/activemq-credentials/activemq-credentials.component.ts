import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import {UtilsService} from "../shared/utils.service";
import {ReportingComponent} from "../reporting/reporting.component";

@Component({
  selector: 'app-activemq-credentials',
  templateUrl: './activemq-credentials.component.html',
  styleUrls: ['./activemq-credentials.component.scss']
})
export class ActivemqCredentialsComponent implements OnInit {
  public title = ''
  buttonTitle!: string;
  error: boolean = false;
  cloudCredsForm!: FormGroup;
  username: string = '';
  password: string = '';
  confirmPassword: string = '';
  mqHost: string = "";
  updateExisting: boolean = false;

  @ViewChild(ReportingComponent) reporting: ReportingComponent = new ReportingComponent();

  constructor(private utilsService: UtilsService) {
  }

  getFormControl(fcName: string): FormControl {
    return this.cloudCredsForm.get(fcName) as FormControl;
  }

  anyInvalid(): boolean {
    return this.cloudCredsForm.invalid;
  }

  hideRegisterForm() {
    window.location.href = "#/";
  }

  /**
   * passwordValidator: Called as the main password validator. This is also used to revalidate
   *                    the confirmPassword field against the updated password.
   */
  passwordValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.password = control.value;
      // Update the validation status of the confirmPassword field
      if (this.cloudCredsForm !== undefined) {
        let cpControl: AbstractControl | null = this.cloudCredsForm.get("confirmPassword");
        cpControl?.updateValueAndValidity();
      }
      let invalid = !this.utilsService.activeMQPasswordRegex.test(control.value);
      let error: any = {pattern: {value: control.value}};
      return invalid ? error : null;
    };
  }

  mqHostValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.mqHost = control.value;
      const invalid = !this.utilsService.hostNameRegex.test(control.value) &&
        !this.utilsService.ipV4RegEx.test(control.value) &&
        !this.utilsService.ipV6RegEx.test(control.value);
      return invalid ? {invalidHost: {value: control.value}} : null;
    };
  }

  passwordMatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.confirmPassword = control.value;
      const ok = this.password !== control.value;
      return ok ? {notMatching: {value: control.value}} : null;
    };
  }

  formValidator() : {} {
    const retval: {valid: boolean,
      fc: FormControl | undefined} = {valid: true, fc: undefined};

    const password = this.getFormControl('password').value;
    const username = this.getFormControl('username').value;

    if(username === "" && password !== "") {
      retval.valid = false;
      retval.fc = this.getFormControl("username");
      retval.fc.setErrors({emptyWithPassword: {value:"Nonempty Password"}});
    }
    else if(username !== "" && password === "") {
      retval.valid = false;
      retval.fc = this.getFormControl("password");
      retval.fc.setErrors({emptyWithUsername: {value:"Nonempty username"}});
    }
    else
      this.register();

    return retval;
  }
  confirmOnReturn($event: InputEvent) {
    // Ensure password field is up-to-date for the confirmPassword validity check
    this.password = this.getFormControl('password').value;

    if ($event.inputType == 'insertLineBreak' && !this.anyInvalid())
      this.register();
  }

  register() {
    this.reporting.dismiss();

    this.username = this.getFormControl('username').value;

    this.utilsService.addOrUpdateActiveMQCreds(this.username, this.password, this.confirmPassword, this.mqHost).subscribe(
      {
        complete: () => {
          this.utilsService.getHasLocalAccount();
          this.reporting.successMessage = "ActiveMQ client credentials " + (this.updateExisting ? " updated" : " created") + " successfully" + (this.updateExisting ? " username now: " + this.username : "");
        },
        error: (reason) => {
          this.reporting.errorMessage = reason;
        }
      });
  }

  checkForActiveMQACreds() {
    this.utilsService.checkForActiveMQCreds().subscribe({
      next: (value: { hasActiveMQCreds: boolean, mqHost: string }) => {
        this.updateExisting = value.hasActiveMQCreds;
        this.title = value.hasActiveMQCreds ? "Update ActiveMQ Credentials" : "Enter ActiveMQ Credentials";
        this.buttonTitle = value.hasActiveMQCreds ? "Update Creds" : "Confirm Creds";
        this.mqHost = value.mqHost;
        this.getFormControl('mqHost').setValue(this.mqHost);
        this.error = false;
      },
      error: (reason) => {
        this.reporting.errorMessage = reason;
        this.title = "Problem!"
        this.buttonTitle = "Problem!";
        this.error = true;
      }
    });
  }

  ngOnInit(): void {
    this.cloudCredsForm = new FormGroup({
      username: new FormControl(this.username, [Validators.maxLength(20), Validators.pattern("^$|^[a-zA-Z0-9](_(?!(\.|_))|\.(?!(_|\.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$")]),
      password: new FormControl(this.password, [Validators.maxLength(20), this.passwordValidator()]),
      confirmPassword: new FormControl(this.confirmPassword, [Validators.maxLength(20), this.passwordMatchValidator()]),
      mqHost: new FormControl(this.mqHost, [Validators.required, Validators.maxLength(39), this.mqHostValidator()]),
    }, {updateOn: "change"});

    // Ensure camera form controls highlight immediately if invalid
    this.cloudCredsForm.markAllAsTouched();
    this.checkForActiveMQACreds();
  }
}
