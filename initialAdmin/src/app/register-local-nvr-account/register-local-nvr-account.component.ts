import {AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import {ReportingComponent} from "../reporting/reporting.component";
import {UtilsService} from '../shared/utils.service';

@Component({
  selector: 'app-register-local-nvr-account',
  templateUrl: './register-local-nvr-account.component.html',
  styleUrls: ['./register-local-nvr-account.component.scss']
})
export class RegisterLocalNvrAccountComponent implements OnInit, AfterViewInit {
  username: string = '';
  password: string = '';
  confirmPassword: string = '';
  email: string = '';
  confirmEmail: string = '';
  updateExisting: boolean = false;
  nvrAccountRegistrationForm!: FormGroup;
  callFailed: boolean = false;
  committed: boolean = false;
  // errorMessage: string = '';
  // successMessage: string = '';
  @ViewChild('username') usernameInput!: ElementRef<HTMLInputElement>;

  // Assigning this here so as not to use ?. in the template when referencing the reporting
  //  component. Angular 15 complains about ?. when reporting was declared not being null
  @ViewChild(ReportingComponent) reporting: ReportingComponent = new ReportingComponent();
  title!: string;
  buttonTitle!: string;
  error: boolean = false;


  constructor(private utilsService: UtilsService) {
  }

  /**
   * passwordValidator: Called as the main password validator. This is also used to revalidate
   *                    the confirmPassword field against the updated password.
   */
  passwordValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.password = control.value;
      // Update the validation status of the confirmPassword field
      if (this.confirmPassword !== "") {
        let cpControl: AbstractControl | null = this.nvrAccountRegistrationForm.get("confirmPassword");
        cpControl?.updateValueAndValidity();
      }

      const ok = !this.utilsService.passwordRegex.test(control.value);
      return ok ? {pattern: {value: control.value}} : null;
    };
  }

  passwordMatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.confirmPassword = control.value;
      const ok = this.password !== control.value;
      return ok ? {notMatching: {value: control.value}} : null;
    };
  }

  emailValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.email = control.value;
      // Update the validation status of the confirmEmail field
      if (this.confirmEmail !== "") {
        let cpControl: AbstractControl | null = this.nvrAccountRegistrationForm.get("confirmEmail");
        cpControl?.updateValueAndValidity();
      }

      const ok = !new RegExp("^([a-zA-Z0-9_\\-.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(]?)$").test(control.value);
      return ok ? {pattern: {value: control.value}} : null;
    };
  }

  emailMatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      this.confirmEmail = control.value;
      const ok = this.email !== control.value;
      return ok ? {notMatching: {value: control.value}} : null;
    };
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
    this.callFailed = this.committed = false;
    this.utilsService.createOrUpdateLocalNVRAccount(this.username, this.password, this.confirmPassword, this.email, this.confirmEmail, this.updateExisting).subscribe(
      {complete: () => {
        this.utilsService.getHasLocalAccount();
        this.reporting.successMessage = "Local client account " + (this.updateExisting ? " updated":" created") + " successfully"+ (this.updateExisting?" username now: "+this.username:"");
        this.committed = true;
      },
      error: (reason) => {
        this.reporting.errorMessage = reason;
        this.callFailed = true;
      }});
  }

  getFormControl(fcName: string): FormControl {
    return this.nvrAccountRegistrationForm.get(fcName) as FormControl;
  }

  anyInvalid(): boolean {
    return this.nvrAccountRegistrationForm.invalid;
  }

  hideRegisterForm() {
    window.location.href = "#/";
  }

  checkForLocalAccount() {
    this.callFailed = false;
    this.utilsService.checkForAccountLocally().subscribe({
      next: (value: boolean) => {
        this.updateExisting = value;
        this.title = value ? "Update the existing NVR account" : "Register a New NVR Account";
        this.buttonTitle = value ? "Update Account" : "Register Account";
        this.error = false;
      },
      error: (reason) => {
        this.reporting.errorMessage = reason;
        this.title = "Problem!"
        this.buttonTitle = "Problem!";
        this.error = this.callFailed = true;
      }
    });
  }

  ngOnInit(): void {
    this.nvrAccountRegistrationForm = new FormGroup({
      username: new FormControl(this.username, [Validators.required, Validators.maxLength(20), Validators.pattern("^[a-zA-Z0-9](_(?!(\.|_))|\.(?!(_|\.))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$")]),
      password: new FormControl(this.password, [Validators.required, Validators.maxLength(25), this.passwordValidator()]),
      confirmPassword: new FormControl(this.confirmPassword, [Validators.required, Validators.maxLength(25), this.passwordMatchValidator()]),
      email: new FormControl(this.email, [Validators.required, Validators.maxLength(40), this.emailValidator()]),
      confirmEmail: new FormControl(this.confirmEmail, [Validators.required, Validators.maxLength(40), this.emailMatchValidator()])
    }, {updateOn: "change"});

    // Ensure camera form controls highlight immediately if invalid
    this.nvrAccountRegistrationForm.markAllAsTouched();
    this.checkForLocalAccount();
  }

  ngAfterViewInit(): void {
    // Set the focus to the username input
    this.usernameInput.nativeElement.focus();
  }
}
