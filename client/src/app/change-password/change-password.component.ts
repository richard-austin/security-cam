import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, ValidatorFn, Validators} from "@angular/forms";
import {timer} from "rxjs";

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

  constructor() { }

  changePasswordButtonDisabled():boolean {
      return false;
  }

  hasError = (controlName: string, errorName: string):boolean =>{
    let x = this.changePasswordForm.controls[controlName].hasError(errorName);
    return x;
  }

  private newPasswordValue():string {
    return 'confirm'; //this.newPassword ? this.newPassword.nativeElement.value : '';
  }

  formSubmitted() {

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

    timer(10000).subscribe(() => this.invalidPassword());
  }
}
