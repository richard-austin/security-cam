import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {ReportingComponent} from "../reporting/reporting.component";
import { HttpErrorResponse } from "@angular/common/http";
import { UtilsService } from '../shared/utils.service';

@Component({
    selector: 'app-change-password2',
    templateUrl: './change-password.component.html',
    styleUrls: ['./change-password.component.scss'],
    standalone: false
})
export class ChangePasswordComponent implements OnInit {

  changePasswordForm!: UntypedFormGroup;
  @ViewChild(ReportingComponent) reporting!:ReportingComponent;

  constructor(private utilsService: UtilsService) { }

  changePasswordButtonDisabled():boolean {
    return this.anyInvalid();
  }

  hasError = (controlName: string, errorName: string):boolean =>{
    return this.changePasswordForm.controls[controlName].hasError(errorName);
  }

  formSubmitted() {
    let oldPassword:AbstractControl = this.changePasswordForm.controls['oldPassword'];
    let newPassword:AbstractControl = this.changePasswordForm.controls['newPassword'];
    let confirmNewPassword: AbstractControl = this.changePasswordForm.controls['confirmNewPassword'];

    this.utilsService.changePassword(oldPassword.value, newPassword.value, confirmNewPassword.value).subscribe(() => {
          this.reporting.successMessage="Password changed";
        },
        (reason: HttpErrorResponse) => {
          if(reason.status === 400)
          {
            for(const key of Object.keys(reason.error)) {
              if(key === 'oldPassword')
                this.invalidPassword();
            }
            this.reporting.errorMessage = reason;
          }
          else
            this.reporting.errorMessage = reason;
        });
  }

  invalidPassword()
  {
    let oldPasswordCtl: AbstractControl = this.changePasswordForm.controls['oldPassword'];
    let errors:{[key: string]: any} = {pattern: {badPassword:"Password Incorrect"}};
    oldPasswordCtl.setErrors(errors);
    oldPasswordCtl.markAsTouched({onlySelf: true}); //updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  /**
   * comparePasswords: Custom form field validator to check new password and confirm new password
   *                   are equal
   * @param control
   */
  comparePasswords(control: AbstractControl): { [key: string]: boolean } | null {
    let fg: UntypedFormGroup=control.parent as UntypedFormGroup;
    let ac: AbstractControl = fg?.controls['newPassword'];
    if (control.value !== undefined && control.value !== ac?.value) {
      return { 'confirmNewPassword': true };
    }
    return null;
  }

  anyInvalid(): boolean{
    return this.changePasswordForm.invalid;
  }

  ngOnInit(): void {
    this.changePasswordForm = new UntypedFormGroup({
      oldPassword: new UntypedFormControl('', [Validators.required]),
      newPassword: new UntypedFormControl('', [Validators.required, Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/)]),
      confirmNewPassword: new UntypedFormControl('', [Validators.required, this.comparePasswords])
    }, {updateOn: "change"});
    this.changePasswordForm.markAllAsTouched();
  }
}
