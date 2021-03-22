import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, ValidatorFn, Validators} from "@angular/forms";
import {timer} from "rxjs";
import {ChangePasswordService} from "./change-password.service";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent implements OnInit {

  changePasswordForm!: FormGroup;
  @ViewChild('oldPassword') oldPasswordEl!: ElementRef<HTMLInputElement>;
  @ViewChild('newPassword') newPassword!: ElementRef<HTMLInputElement>;
  @ViewChild('confirmNewPassword') confirmNewPassword!: ElementRef<HTMLInputElement>;
  @ViewChild(ReportingComponent) reporting!:ReportingComponent;

  constructor(private changePasswordService:ChangePasswordService) { }

  changePasswordButtonDisabled():boolean {
    // for(const key of Object.keys(this.changePasswordForm.controls))
    // {
    //   if(this.hasError(key, 'required') || this.hasError(key, 'pattern'))
    //     return true;
    // }
    return false;
  }

  hasError = (controlName: string, errorName: string):boolean =>{
    return this.changePasswordForm.controls[controlName].hasError(errorName);
  }

  formSubmitted() {
    let oldPassword:AbstractControl = this.changePasswordForm.controls['oldPassword'];
    let newPassword:AbstractControl = this.changePasswordForm.controls['newPassword'];
    let confirmNewPassword: AbstractControl = this.changePasswordForm.controls['confirmNewPassword'];

    this.changePasswordService.changePassword(oldPassword.value, newPassword.value, confirmNewPassword.value).subscribe(() => {
      this.reporting.successMessage="Password changed";
    },
    (reason: HttpErrorResponse) => {
      if(reason.status === 400)
      {
        for(const key of Object.keys(reason.error)) {
            if(key === 'oldPassword')
              this.invalidPassword();
        }
      }
      else
        this.reporting.error = reason;
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
    let fg: FormGroup=control.parent as FormGroup;
    let ac: AbstractControl = fg?.controls['newPassword'];
    if (control.value !== undefined && control.value !== ac?.value) {
      return { 'confirmNewPassword': true };
    }
    return null;
  }

  ngOnInit(): void {
    this.changePasswordForm = new FormGroup({
      oldPassword: new FormControl('', [Validators.required]),
      newPassword: new FormControl('', [Validators.required, Validators.pattern(/^[-\[\]!\"#$%&\'()*+,.\/:;<=>?@^_\`{}|~\\0-9A-Za-z]{1,64}$/)]),
      confirmNewPassword: new FormControl('', [this.comparePasswords])
    });
  }
}
