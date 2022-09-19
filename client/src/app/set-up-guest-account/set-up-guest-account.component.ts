import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, Validators} from "@angular/forms";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";
import {UtilsService} from '../shared/utils.service';
import {MatCheckboxChange} from '@angular/material/checkbox';

@Component({
  selector: 'app-set-up-guest-account',
  templateUrl: './set-up-guest-account.component.html',
  styleUrls: ['./set-up-guest-account.component.scss']
})
export class SetUpGuestAccountComponent implements OnInit {
  setupGuestAccountForm!: FormGroup;
  @ViewChild(ReportingComponent) reporting!: ReportingComponent;

  constructor(private utilsService: UtilsService) {
  }

  submitButtonDisabled(): boolean {
    return this.anyInvalid();
  }

  hasError = (controlName: string, errorName: string): boolean => {
    return this.setupGuestAccountForm.controls[controlName].hasError(errorName);
  }

  formSubmitted() {
    let enabled: AbstractControl = this.setupGuestAccountForm.controls['enabled'];
    let password: AbstractControl = this.setupGuestAccountForm.controls['password'];
    let confirmPassword: AbstractControl = this.setupGuestAccountForm.controls['confirmPassword'];

    this.utilsService.setupGuestAccount(enabled.value, password.value, confirmPassword.value).subscribe(() => {
        this.reporting.successMessage = "Guest account settings changed";
      },
      (reason: HttpErrorResponse) => {
        if (reason.status === 400) {
          for (const key of Object.keys(reason.error)) {
            if (key === 'password')
              this.invalidPassword();
          }
          this.reporting.errorMessage = reason;
        } else
          this.reporting.errorMessage = reason;
      });
  }

  invalidPassword() {
    let passwordCtl: AbstractControl = this.setupGuestAccountForm.controls['password'];
    let errors: { [key: string]: any } = {pattern: {badPassword: "Password Incorrect"}};
    passwordCtl.setErrors(errors);
    passwordCtl.markAsTouched({onlySelf: true}); //updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  /**
   * comparePasswords: Custom form field validator to check new password and confirm new password
   *                   are equal
   * @param control
   */
  comparePasswords(control: AbstractControl): { [key: string]: boolean } | null {
    let fg: FormGroup = control.parent as FormGroup;
    let ac: AbstractControl = fg?.controls['password'];
    if (control.value !== undefined && control.value !== ac?.value) {
      return {'confirmPassword': true};
    }
    return null;
  }

  anyInvalid(): boolean {
    return this.setupGuestAccountForm.invalid;
  }

  setEnabled($event: MatCheckboxChange) {
    this.setupGuestAccountForm.controls['enabled'].setValue($event.checked);
  }

  async ngOnInit(): Promise<void> {
    this.setupGuestAccountForm = new FormGroup({
      enabled: new FormControl('', []),
      password: new FormControl('', [Validators.pattern(/^$|^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/)]),
      confirmPassword: new FormControl('', [this.comparePasswords])
    }, {updateOn: "change"});
    this.setupGuestAccountForm.markAllAsTouched();
    this.setupGuestAccountForm.controls['enabled'].setValue((await this.utilsService.guestAccountEnabled()).enabled);
  }
}
